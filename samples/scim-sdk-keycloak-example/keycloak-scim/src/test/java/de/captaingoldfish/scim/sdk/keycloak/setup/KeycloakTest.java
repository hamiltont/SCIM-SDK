package de.captaingoldfish.scim.sdk.keycloak.setup;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.ClientScopeAttributeEntity;
import org.keycloak.models.jpa.entities.ClientScopeClientMappingEntity;
import org.keycloak.models.jpa.entities.ClientScopeEntity;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.GroupRoleMappingEntity;
import org.keycloak.models.jpa.entities.ProtocolMapperEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleAttributeEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;

import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Slf4j
public abstract class KeycloakTest
{

  /**
   * initializes the database
   */
  private static final DatabaseSetup DATABASE_SETUP = new DatabaseSetup();

  /**
   * creates a default configuration that we are using in our unit tests
   */
  private static final KeycloakMockSetup KEYCLOAK_MOCK_SETUP = new KeycloakMockSetup(DATABASE_SETUP.getKeycloakSession(),
                                                                                     DATABASE_SETUP.getEntityManager());

  /**
   * the custom realm for our unit tests
   */
  public RealmModel getRealmModel()
  {
    return KEYCLOAK_MOCK_SETUP.getRealmModel();
  }

  /**
   * the mocked keycloak session
   */
  public KeycloakSession getKeycloakSession()
  {
    return DATABASE_SETUP.getKeycloakSession();
  }

  /**
   * the entitymanager that we and the keycloak tools will use to read and store entities within the database
   */
  public EntityManager getEntityManager()
  {
    return DATABASE_SETUP.getEntityManager();
  }

  /**
   * persists an entity and flushes the current transaction
   */
  public void persist(Object entity)
  {
    getEntityManager().persist(entity);
    getEntityManager().flush();
  }

  /**
   * begin a transaction before each test
   */
  @BeforeEach
  public void initializeKeycloakSetup()
  {
    clearTables();
    KEYCLOAK_MOCK_SETUP.createRealm();
    beginTransaction();
  }

  /**
   * will destroy the current test-setup by deleting the created entities again for example
   */
  @AfterEach
  public void destroy()
  {
    clearTables();
    getEntityManager().clear(); // clears the cache of the entityManager etc.
    commitTransaction();
  }

  /**
   * used to start a new JPA transaction
   */
  public void beginTransaction()
  {
    if (!getEntityManager().getTransaction().isActive())
    {
      getEntityManager().getTransaction().begin();
    }
  }

  /**
   * commits a transaction if any is active
   */
  public void commitTransaction()
  {
    if (getEntityManager().getTransaction().isActive())
    {
      getEntityManager().getTransaction().commit();
    }
  }

  /**
   * does a rollback on the transaction in cases that an exception has occured due to a unique constraint
   * failure for example
   */
  public void rollbackTransaction()
  {
    if (getEntityManager().getTransaction().isActive())
    {
      getEntityManager().getTransaction().rollback();
    }
  }

  /**
   * this method is used as a clean up operations on the database tables that are used on SCIM testing
   */
  public void clearTables()
  {
    log.debug("cleaning up tables");

    deleteFromMappingTable("CLIENT_ATTRIBUTES");
    deleteFromMappingTable("REDIRECT_URIS");
    deleteFromMappingTable("CLIENT_DEFAULT_ROLES");
    deleteFromMappingTable("SCOPE_MAPPING");
    deleteFromMappingTable("PROTOCOL_MAPPER_CONFIG");

    deleteFromTable(UserGroupMembershipEntity.class);
    deleteFromTable(UserRoleMappingEntity.class);
    deleteFromTable(GroupRoleMappingEntity.class);
    deleteFromTable(UserAttributeEntity.class);
    deleteFromTable(CredentialEntity.class);
    deleteFromTable(UserEntity.class);
    deleteFromTable(GroupAttributeEntity.class);
    deleteFromTable(GroupEntity.class);
    deleteFromTable(ProtocolMapperEntity.class);
    deleteFromTable(ClientScopeClientMappingEntity.class);
    deleteFromTable(ClientScopeAttributeEntity.class);
    deleteFromTable(ClientScopeEntity.class);
    deleteFromTable(RoleAttributeEntity.class);
    deleteFromTable(RoleEntity.class);
    deleteFromTable(ClientEntity.class);
    deleteFromTable(RealmEntity.class);
    log.debug("cleaned tables successfully");
  }

  /**
   * will delete the entries of a single table
   *
   * @param tableName the name of the table that should be deleted
   */
  public void deleteFromMappingTable(String tableName)
  {
    beginTransaction();
    getEntityManager().createNativeQuery("delete from " + tableName).executeUpdate();
    commitTransaction();
  }

  /**
   * will delete the entries of a single table
   *
   * @param entityClass the entity whose entries should be deleted
   */
  public void deleteFromTable(Class<?> entityClass)
  {
    beginTransaction();
    getEntityManager().createQuery("delete from " + entityClass.getSimpleName()).executeUpdate();
    commitTransaction();
  }

  /**
   * counts the number of entries within the given table
   *
   * @param entityClass the class-type of the entity whose entries should be counted
   * @return the number of entries within the database of the given entity-type
   */
  public int countEntriesInTable(Class<?> entityClass)
  {
    return ((Long)getEntityManager().createQuery("select count(entity) from " + entityClass.getSimpleName() + " entity")
                                    .getSingleResult()).intValue();
  }
}
