package ElectronicAssetTradingPlatform.UnitTesting;

import ElectronicAssetTradingPlatform.Database.ETPDataSource;
import ElectronicAssetTradingPlatform.Database.UsersDataSource;
import ElectronicAssetTradingPlatform.Users.ITAdmin;
import ElectronicAssetTradingPlatform.Users.OrganisationalUnitLeader;
import ElectronicAssetTradingPlatform.Users.SystemsAdmin;
import ElectronicAssetTradingPlatform.Users.User;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

public class ITAdminTesting {
    ITAdmin itAdmin;

    @BeforeEach
    @Test
    public void setUpITAdmin() {
        ETPDataSource etp = new ETPDataSource();
        // create an organisational unit member
        itAdmin = new ITAdmin("adminGuy", "pass123", "salt");

    }

    // Create new users tests
    @Test
    public void invalidName() {
        assertThrows(Exception.class, () -> itAdmin.createUser("", "", "ITAdmin"));
        assertThrows(Exception.class, () -> itAdmin.createUser(" ", "", "ITAdmin"));
        assertThrows(Exception.class, () -> itAdmin.createUser(null, "", "ITAdmin"));
    }
    @Test
    public void invalidUnitName() {
        assertThrows(Exception.class, () -> itAdmin.createUser("bob", "", "OrganisationalUnitLeader"));
        assertThrows(Exception.class, () -> itAdmin.createUser("bob", " ", "OrganisationalUnitLeader"));
        assertThrows(Exception.class, () -> itAdmin.createUser("bob", null, "OrganisationalUnitLeader"));
    }
    @Test
    public void invalidUserType() {
        assertThrows(Exception.class, () -> itAdmin.createUser("bob", "", "asd"));
        assertThrows(Exception.class, () -> itAdmin.createUser("bob", "", ""));
        assertThrows(Exception.class, () -> itAdmin.createUser("bob", "", null));
    }
    @Test
    public void validITAdmin() throws Exception {
        assertDoesNotThrow(() -> {
            itAdmin.createUser("newITAdmin1", "", "ITAdmin");
            itAdmin.createUser("newSysAdmin2", "asdf", "ITAdmin");
        });

        assertEquals(itAdmin.createUser("newSysAdmin2", "asdf", "ITAdmin").getClass(), ITAdmin.class);
    }
    @Test
    public void validOrgLeader() throws Exception {
        assertDoesNotThrow(() -> itAdmin.createUser("newLeader", "unit1", "OrganisationalUnitLeader"));

        assertEquals(itAdmin.createUser("newLeader", "unit1", "OrganisationalUnitLeader").getClass(), OrganisationalUnitLeader.class);
    }
    @Test
    public void validOrgMember() throws Exception {
        assertDoesNotThrow(() -> itAdmin.createUser("newMember", "unit1", "OrganisationalUnitMembers"));

        assertEquals(itAdmin.createUser("newLeader", "unit1", "OrganisationalUnitLeader").getClass(), OrganisationalUnitLeader.class);
    }
    @Test
    public void validSystemsAdmin() throws Exception {
        assertDoesNotThrow(() -> {
            itAdmin.createUser("newSysAdmin1", "", "SystemsAdmin");
            itAdmin.createUser("newSysAdmin2", "asdf", "SystemsAdmin");
        });

        assertEquals(itAdmin.createUser("newSysAdmin2", "asdf", "SystemsAdmin").getClass(), SystemsAdmin.class);
    }
    @Test
    public void notDuplicatePassword() throws Exception {
        ITAdmin itAdmin1 = new ITAdmin("adminGuy1", "pass123", "salt");
        ITAdmin itAdmin2 = new ITAdmin("adminGuy2", "pass123", "salt");

        String user1 = itAdmin1.createUser("bob1", "", "ITAdmin").getPassword();
        String user2 = itAdmin2.createUser("bob2", "", "ITAdmin").getPassword();
        String user3 = itAdmin2.createUser("bob3", "", "ITAdmin").getPassword();
        String user4 = itAdmin1.createUser("bob4", "", "ITAdmin").getPassword();

        System.out.println(user1);
        System.out.println(user2);
        System.out.println(user3);
        System.out.println(user4);

        assertNotEquals(user1, user2);
        assertNotEquals(user2, user3);
        assertNotEquals(user1, user4);
    }

    // Edit user tests
    @Test
    public void editUser() throws Exception {
        itAdmin.editUser("user1", "OrganisationalUnitLeader", "Unit1");
    }

    // Users Data Source test
    @Test
    public void insertUser() throws Exception {
        UsersDataSource db = new UsersDataSource();
        User user = itAdmin.createUser("newSysAdmin1", "", "SystemsAdmin");
        db.insertUser(user);

        User dbUser = db.getUser("newSysAdmin1");

        assertEquals(user.getUsername(), dbUser.getUsername());
    }
}
