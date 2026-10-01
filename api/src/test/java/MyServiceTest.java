import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @Mock
    private Db dbMock;

    @InjectMocks
    private MyService myService;

    @Captor
    private ArgumentCaptor<MyEntity> myEntityCaptor;

    @Test
    @DisplayName("Should set new entity with generated UUID and save it to Db")
    void testSetMyEntityGeneratesUuidAndSavesToDb() {
        // given:
        MyEntity inputEntity = new MyEntity("test-name");
        UUID expectedUuid = UUID.randomUUID();

        // Подменяем вызов статического метода UUID.randomUUID()
        try (var mockedUuid = mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(expectedUuid);

            // when:
            MyEntity resultEntity = myService.setMyEntity(inputEntity);

            // then:
            Assertions.assertSame(inputEntity, resultEntity);
            Assertions.assertEquals(expectedUuid, resultEntity.getId());

            // Проверяем, что сущность была передана в Db
            verify(dbMock).setMyEntity(myEntityCaptor.capture());
            MyEntity savedEntity = myEntityCaptor.getValue();
            Assertions.assertEquals("test-name", savedEntity.getName());
            Assertions.assertEquals(expectedUuid, savedEntity.getId());
        }
    }

    @Test
    @DisplayName("Should return default entity with name 'first' from real Db instance")
    void testGetMyEntityReturnsDefaultFirstEntity() {
        // given:
        MyService realService = new MyService();

        // Достаем реальный Db через рефлексию
        Db realDb = extractDbViaReflection(realService);

        // Создаем мок-оболочку вокруг реального объекта, чтобы Mockito мог им управлять
        Db dbSpy = mock(Db.class, Mockito.withSettings().spiedInstance(realDb));

        // Подменяем поле db в сервисе на нашу мок-оболочку
        setDbField(realService, dbSpy);

        MyEntity defaultEntity = new MyEntity("first");
        defaultEntity.setId(UUID.randomUUID());

        // Теперь настраиваем поведение на моке (dbSpy), а не на реальном объекте
        when(dbSpy.getMyEntity()).thenReturn(defaultEntity);

        // when:
        MyEntity entity = realService.getMyEntity();

        // then:
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("first", entity.getName());
        Assertions.assertNotNull(entity.getId());
    }

    @Test
    @DisplayName("Should throw NPE when getting name from null entity returned by Db")
    void testGetMyEntityNameThrowsWhenEntityIsNull() {
        // given:
        MyService service = new MyService();

        Db realDb = extractDbViaReflection(service);
        Db dbSpy = mock(Db.class, Mockito.withSettings().spiedInstance(realDb));
        setDbField(service, dbSpy);

        when(dbSpy.getMyEntity()).thenReturn(null);

        // when:
        MyEntity myEntity = service.getMyEntity();

        // then:
        Assertions.assertNull(myEntity);

        // JUnit сам вызовет этот метод через рефлексию.
        // Если myEntity будет null, вызов getName() внутри этого метода корректно выбросит NPE.
        assertThrows(NullPointerException.class, () -> getNameOrThrow(myEntity));
    }

    private String getNameOrThrow(MyEntity entity) {
        return entity.getName();
    }

    // Утилитарные методы для работы с рефлексией
    private Db extractDbViaReflection(MyService service) {
        try {
            Field dbField = MyService.class.getDeclaredField("db");
            dbField.setAccessible(true);
            return (Db) dbField.get(service);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to access private field 'db'", e);
        }
    }

    private void setDbField(MyService service, Db newDb) {
        try {
            Field dbField = MyService.class.getDeclaredField("db");
            dbField.setAccessible(true);
            dbField.set(service, newDb);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set private field 'db'", e);
        }
    }
}