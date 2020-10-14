package com.springvuegradle.hakinakina.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.springvuegradle.hakinakina.controller.UserController;
import com.springvuegradle.hakinakina.entity.*;
import com.springvuegradle.hakinakina.repository.*;
import com.springvuegradle.hakinakina.service.UserService;
import com.springvuegradle.hakinakina.util.ResponseHandler;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService service;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PassportCountryRepository countryRepository;

    @MockBean
    private HomeFeedRepository homeFeedRepository;

    @MockBean
    private EmailRepository emailRepository;

    @MockBean
    private SessionRepository sessionRepository;

    @MockBean
    private ActivityTypeRepository activityTypeRepository;

    @MockBean
    private ActivityRepository activityRepository;

    @MockBean
    private SearchRepository searchRepository;

    @MockBean
    private UserActivityRoleRepository userActivityRoleRepository;

    @MockBean
    private AchievementRepository achievementRepository;

    @MockBean
    private ResultRepository resultRepository;

    @MockBean
    private LocationRepository locationRepository;

    private ResponseHandler responseHandler = new ResponseHandler();

    private final String EDIT_PROFILE_JSON = "{\n" +
            "  \"lastname\": \"Benson\",\n" +
            "  \"firstname\": \"Maurice\",\n" +
            "  \"middlename\": \"Jack\",\n" +
            "  \"nickname\": \"Jacky\",\n" +
            "  \"primary_email\": \"jacky@google.com\",\n" +
            "  \"bio\": \"Jacky loves to ride his bike on crazy mountains.\",\n" +
            "  \"date_of_birth\": \"1985-12-20\",\n" +
            "  \"gender\": \"male\",\n" +
            "  \"fitness\": 4\n" +
            "}";

    private final String EDIT_PERMISSSION_LEVEL_JSON = "{\n" +
            "  \"lastname\": \"Smith\",\n" +
            "  \"firstname\": \"John\",\n" +
            "  \"middlename\": \"Jack\",\n" +
            "  \"nickname\": \"Jacky\",\n" +
            "  \"primary_email\": \"john@google.com\",\n" +
            "  \"bio\": \"Jacky loves to ride his bike on crazy mountains.\",\n" +
            "  \"date_of_birth\": \"1985-12-20\",\n" +
            "  \"gender\": \"male\",\n" +
            "  \"permission_level\": 1\n" +
            "}";

    @Test
    public void userCreationSuccess() throws Exception {
        String input = "{\n" +
                "  \"lastname\": \"Qiu\",\n" +
                "  \"firstname\": \"Jackie\",\n" +
                "  \"middlename\": \"Danger\",\n" +
                "  \"nickname\": \"J\",\n" +
                "  \"primary_email\": \"jqi26@uclive.ac.nz\",\n" +
                "  \"password\": \"Password1\",\n" +
                "  \"bio\": \"bio\",\n" +
                "  \"date_of_birth\": \"1985-12-20\",\n" +
                "  \"gender\": \"male\",\n" +
                "  \"fitness\": 2\n" +
                "}";

        when(service.validateCreateProfile(any(User.class), any(HttpServletResponse.class))).thenReturn(responseHandler
                .formatSuccessResponse(200, "User updated"));
        this.mockMvc.perform(post("/profiles").contentType(MediaType.APPLICATION_JSON)
                .content(input)).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("User updated")));
    }

    @Test
    public void loginSuccess() throws Exception {
        String input = "{\n" +
                "  \"email\": \"my@email.com\",\n" +
                "  \"password\": \"mysecurepwd\"\n" +
                "}";
        when(service.checkLogin(eq("my@email.com"), eq("mysecurepwd"), any(HttpServletResponse.class)))
                .thenReturn(new ResponseEntity("{}", HttpStatus.valueOf(200)));
        this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content(input)).andDo(print()).andExpect(status().is(200));
    }

    @Test
    public void loginFail() throws Exception {
        String input = "{\n" +
                "  \"email\": \"my@email.com\",\n" +
                "  \"password\": \"mysecurepwd\"\n" +
                "}";
        when(service.checkLogin(eq("my@email.com"), eq("mysecurepwd"), any(HttpServletResponse.class)))
                .thenReturn(new ResponseEntity("Incorrect password", HttpStatus.FORBIDDEN));
        this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content(input)).andDo(print()).andExpect(status().is(403))
                .andExpect(content().string(containsString("Incorrect password")));
    }

    @Test
    public void logoutSuccess() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        doNothing().when(sessionRepository).removeToken(any(String.class));
        this.mockMvc.perform(post("/logout").cookie(tokenCookie))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("User logged out")));
    }

    @Test
    public void getUserByIdTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");

        // create another default admin
        User defaultAdmin = new User();
        defaultAdmin.setPermissionLevel(2);

        testUser.setUserId((long) 1);
        testUser.resetPassportCountries();
        testSession.setUser(defaultAdmin);

        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(testSession);
        when(userRepository.findById((long) 1)).thenReturn(Optional.of(testUser));
        when(userRepository.getUserById((long) 1)).thenReturn(Optional.of(testUser));
        MvcResult mvcResult = this.mockMvc.perform(get("/profiles/1").cookie(tokenCookie))
                .andExpect(status().is(200))
                .andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertEquals("john@gmail.com", JsonPath.parse(response).read("$.primary_email"));
    }

    @Test
    public void getOneUserFailTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");

        // create another default admin
        User defaultAdmin = new User();
        defaultAdmin.setPermissionLevel(2);

        testUser.setUserId((long) 1);
        testUser.resetPassportCountries();
        testSession.setUser(defaultAdmin);

        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(testSession);
        when(userRepository.findById((long) 1)).thenReturn(Optional.of(testUser));
        when(userRepository.getUserById((long) 1)).thenReturn(Optional.of(testUser));
        this.mockMvc.perform(get("/profiles/999").cookie(tokenCookie))
                .andExpect(status().is(404))
                .andExpect(content().string(containsString("User does not exist")));
    }

    @Test
    public void getAllCountriesTest() throws Exception {
        PassportCountry country1 = new PassportCountry("NZ", "New Zealand");
        PassportCountry country2 = new PassportCountry("AU", "Australia");
        ArrayList<PassportCountry> countries = new ArrayList<>();
        countries.add(country1);
        countries.add(country2);
        when(countryRepository.findAll()).thenReturn(countries);
        this.mockMvc.perform(get("/countries"))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("[New Zealand, Australia]")));
    }

    @Test
    public void editEmailTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        String input = "{\n" +
                "     *   \"primary_email\": \"triplej@google.com\",\n" +
                "     *   \"additional_email\": [\n" +
                "     *     \"triplej@xtra.co.nz\",\n" +
                "     *     \"triplej@msn.com\"\n" +
                "     *   ]\n" +
                "     * }";
        when(service.editEmail(any(String.class), any(Long.class), any(String.class))).thenReturn(
                responseHandler.formatSuccessResponse(200, "Primary email switched successfully"));
        this.mockMvc.perform(put("/profiles/1/emails").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("Primary email switched successfully")));
    }

    @Test
    public void validateLoginTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");
        testUser.setUserId((long) 1);
        testUser.resetPassportCountries();
        testSession.setUser(testUser);

        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(testSession);
        this.mockMvc.perform(get("/validateLogin").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200));
    }

    @Test
    public void validateLoginTestNotLoggedIn() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "wrongToken");
        when(sessionRepository.findUserIdByToken("wrongToken")).thenReturn(null);
        this.mockMvc.perform(get("/validateLogin").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401))
                .andExpect(content().string(containsString("User not currently logged in")));
    }

    @Test
    public void getUserByEmailTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");
        testUser.setUserId((long) 1);
        testUser.resetPassportCountries();
        testSession.setUser(testUser);

        String input = "{\"email\": \"john@gmail.com\"}";

        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(testSession);
        when(userRepository.getIdByEmail("john@gmail.com")).thenReturn(Long.valueOf("1"));
        this.mockMvc.perform(get("/email/id/").cookie(tokenCookie)
                .param("email", "john@gmail.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200));
    }

    @Test
    public void addEmailsTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        String input = "{\n" +
                "     *   \"primary_email\": \"triplej@google.com\",\n" +
                "     *   \"additional_email\": [\n" +
                "     *     \"triplej@xtra.co.nz\",\n" +
                "     *     \"triplej@msn.com\"\n" +
                "     *   ]\n" +
                "     * }";
        when(service.addEmails(any(String.class), any(Long.class), any(String.class))).thenReturn(
                responseHandler.formatSuccessResponse(201, "New emails successfully added"));
        this.mockMvc.perform(post("/profiles/1/emails").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().is(201))
                .andExpect(content().string(containsString("New emails successfully added")));
    }

    @Test
    public void getAllEmailsTest() throws Exception {
        ArrayList<String> emails = new ArrayList<>();
        emails.add("john@mail.com");
        emails.add("jane@mail.com");
        when(userRepository.getAllPrimaryEmails()).thenReturn(emails);
        this.mockMvc.perform(get("/emails"))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("[john@mail.com, jane@mail.com]")));
    }

    @Test
    public void editUserSuccessTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");
        testUser.setUserId((long) 1);
        testUser.resetPassportCountries();
        testSession.setUser(testUser);

        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(testSession);
        when(userRepository.findById((long) 1)).thenReturn(Optional.of(testUser));
        when(service.validateEditUser(any(User.class)))
                .thenReturn(responseHandler.formatSuccessResponse(200, "User updated"));
        this.mockMvc.perform(put("/profiles/1").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(EDIT_PROFILE_JSON))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("User updated")));
    }

    /**
     * An admin can edit any user profile registered in the system.
     */
    @Test
    public void editUserSuccessByAdminUserTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 1, "Password1");

        // create another default admin
        User defaultAdmin = new User();
        defaultAdmin.setPermissionLevel(2);

        testUser.setUserId((long) 1);
        testUser.resetPassportCountries();
        testSession.setUser(defaultAdmin);

        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(testSession);
        when(userRepository.findById((long) 1)).thenReturn(Optional.of(testUser));
        when(service.validateEditUser(any(User.class)))
                .thenReturn(responseHandler.formatSuccessResponse(200, "User updated"));
        this.mockMvc.perform(put("/profiles/1").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(EDIT_PROFILE_JSON))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("User updated")));
    }

    @Test
    public void editUserTokenDoesNotMatchAnySessionTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(null);
        this.mockMvc.perform(put("/profiles/1").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(EDIT_PROFILE_JSON))
                .andExpect(status().is(400))
                .andExpect(content().string(containsString("Invalid Session")));
    }

    @Test
    public void editUserTokenMismatchTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");
        testUser.setUserId((long) 1);
        testUser.resetPassportCountries();
        testSession.setUser(testUser);

        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(testSession);
        this.mockMvc.perform(put("/profiles/2").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(EDIT_PROFILE_JSON))
                .andExpect(status().is(400))
                .andExpect(content().string(containsString("Session mismatch")));
    }

    /**
     * A default admin can edit any user permission level registered in the system.
     */
    @Test
    public void editUserPermissionLevelByDefaultAdminSuccessTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        // When the default admin try to change the normal user to the admin, it should success
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 1, "Password1");

        // create another default admin
        User defaultAdmin = new User();
        defaultAdmin.setPermissionLevel(2);

        testUser.setUserId((long) 1);
        testUser.setPermissionLevel(0);
        testUser.resetPassportCountries();
        testSession.setUser(defaultAdmin);

        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(testSession);
        when(userRepository.findById((long) 1)).thenReturn(Optional.of(testUser));
        when(service.validateEditUser(any(User.class)))
                .thenReturn(responseHandler.formatSuccessResponse(200, "User updated"));
        this.mockMvc.perform(put("/profiles/1").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(EDIT_PERMISSSION_LEVEL_JSON))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("User updated")));
    }

    /**
     * An admin(not default admin) or user should not be able to
     * edit any user permission level registered in the system.
     */
    @Test
    public void editUserPermissionLevelByAdminFailTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        // When the admin try to change the normal user to the admin, it should fail
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 1, "Password1");

        // create another admin who cannot edit user permission level
        User defaultAdmin = new User();
        defaultAdmin.setPermissionLevel(1);

        testUser.setUserId((long) 1);
        testUser.setPermissionLevel(0);
        testUser.resetPassportCountries();
        testSession.setUser(defaultAdmin);

        when(sessionRepository.findUserIdByToken("t0k3n")).thenReturn(testSession);
        when(userRepository.findById((long) 1)).thenReturn(Optional.of(testUser));
        this.mockMvc.perform(put("/profiles/1").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(EDIT_PERMISSSION_LEVEL_JSON))
                .andExpect(status().is(401))
                .andExpect(content().string(containsString("Unauthorized: Only the default admin can edit the user permission level")));
    }


    @Test
    public void editPasswordSuccessTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        String input = "{\n" +
                "  \"old_password\": \"myoldpwd\",\n" +
                "  \"new_password\": \"mynewpwd\",\n" +
                "  \"repeat_password\": \"mynewpwd\"\n" +
                "}";
        when(service.changePassword(1, "t0k3n", "myoldpwd", "mynewpwd"))
                .thenReturn(responseHandler.formatSuccessResponse(
                        200, "Successfully changed the password"));
        this.mockMvc.perform(put("/profiles/1/password").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("Successfully changed the password")));
    }

    @Test
    public void editPasswordRepeatMismatchTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        String input = "{\n" +
                "  \"old_password\": \"myoldpwd\",\n" +
                "  \"new_password\": \"mynewpwd\",\n" +
                "  \"repeat_password\": \"mynewerpwd\"\n" +
                "}";
        this.mockMvc.perform(put("/profiles/1/password").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().is(400))
                .andExpect(content().string(containsString("newPassword and repeatPassword do no match")));
    }

    @Test
    public void editActivityTypesTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        String input = "{\n" +
                "  \"activities\": [\n" +
                "    \"Relaxing\",\n" +
                "    \"Fun\"\n" +
                "  ]\n" +
                "}";
        when(service.editActivityTypes(anyList(), anyLong(), anyString()))
                .thenReturn(new ResponseEntity("Successfully updated activity types", HttpStatus.valueOf(200)));
        this.mockMvc.perform(put("/profiles/1/activity-types").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("Successfully updated activity types")));
    }

    @Test
    public void editActivityTypesNonExistentUserTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        String input = "{\n" +
                "  \"activities\": [\n" +
                "    \"Relaxing\",\n" +
                "    \"Fun\"\n" +
                "  ]\n" +
                "}";
        when(service.editActivityTypes(anyList(), anyLong(), eq("t0k3n")))
                .thenReturn(new ResponseEntity("No user with that ID", HttpStatus.valueOf(401)));
        this.mockMvc.perform(put("/profiles/1/activity-types").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().is(401))
                .andExpect(content().string(containsString("No user with that ID")));
    }

    @Test
    public void editActivityTypesNonExistentActivityTypeTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        String input = "{\n" +
                "  \"activities\": [\n" +
                "    \"Relaxing\",\n" +
                "    \"Fun\"\n" +
                "  ]\n" +
                "}";
        when(service.editActivityTypes(anyList(), anyLong(), eq("t0k3n")))
                .thenReturn(new ResponseEntity("Activity type doesn't exist", HttpStatus.valueOf(400)));
        this.mockMvc.perform(put("/profiles/1/activity-types").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().is(400))
                .andExpect(content().string(containsString("Activity type doesn't exist")));
    }

    @Test
    public void editActivityTypesNotListTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        String input = "{\n" +
                "  \"activities\": \"Relaxing\"\n" +
                "}";
        this.mockMvc.perform(put("/profiles/1/activity-types").cookie(tokenCookie)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().is(400))
                .andExpect(content().string(containsString("Must send a list of activities")));
    }

    @Test
    public void parseActivityListTest() throws JsonProcessingException {
        String input = "{\n" +
                "  \"activities\": [\n" +
                "    \"Relaxing\",\n" +
                "    \"Fun\"\n" +
                "  ]\n" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode activitiesNode = mapper.readTree(input).get("activities");
        List<String> result = UserController.parseActivityList(activitiesNode);

        assertEquals(2, result.size());
        assertTrue(result.contains("Relaxing"));
        assertTrue(result.contains("Fun"));
    }

    @Test
    public void getActivityTypesTest() throws Exception {
        List<ActivityType> activityTypes = new ArrayList<>();
        activityTypes.add(new ActivityType("Fun"));
        activityTypes.add(new ActivityType("Relaxing"));
        activityTypes.add(new ActivityType("Extreme"));
        when(activityTypeRepository.findAll()).thenReturn(activityTypes);

        this.mockMvc.perform(get("/activity-types")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(content().json("[Fun, Relaxing, Extreme]"));
    }

    /**
     * User should be able to delete their own account
     */
    @Test
    public void deleteUserTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");
        testUser.setUserId((long) 1);
        testUser.setPermissionLevel(0);
        testSession.setUser(testUser);

        when(service.deleteUser(eq(1L), eq("t0k3n"), any(HttpServletResponse.class)))
                .thenReturn(new ResponseEntity("Successfully Deleted User", HttpStatus.valueOf(200)));
        this.mockMvc.perform(delete("/profiles/1").cookie(tokenCookie))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("Successfully Deleted User")));
    }

    /**
     * A normal registered user should not be able to delete another user's account
     */
    @Test
    public void deleteUserNotAdminTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");
        testUser.setUserId((long) 1);
        testUser.setPermissionLevel(0);

        User normalUser = new User();
        normalUser.setPermissionLevel(0);
        testSession.setUser(normalUser);

        when(service.deleteUser(eq(1L), eq("t0k3n"), any(HttpServletResponse.class)))
                .thenReturn(new ResponseEntity("Unauthorised User", HttpStatus.valueOf(403)));
        this.mockMvc.perform(delete("/profiles/1").cookie(tokenCookie))
                .andExpect(status().is(403))
                .andExpect(content().string(containsString("Unauthorised User")));
    }

    /**
     * An admin should be able to delete another user's account
     */
    @Test
    public void deleteUserAsAdminTest() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");
        testUser.setUserId((long) 1);
        testUser.setPermissionLevel(0);

        User adminUser = new User();
        adminUser.setPermissionLevel(1);
        testSession.setUser(adminUser);

        when(service.deleteUser(eq(1L), eq("t0k3n"), any(HttpServletResponse.class)))
                .thenReturn(new ResponseEntity("Successfully Deleted User", HttpStatus.valueOf(200)));
        this.mockMvc.perform(delete("/profiles/1").cookie(tokenCookie))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("Successfully Deleted User")));
    }

    /***
     * Users should be able to follow activities
     */
    @Test
    public void subscribeToActivity() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");
        testUser.setUserId((long) 1);
        testUser.setPermissionLevel(0);
        testSession.setUser(testUser);

        Activity testActivity = new Activity("testActivity", "Used for testing", true,
                null, null);
        testActivity.setId((long) 1);

        when(service.subscribeToActivity(eq(1L), eq(1L), eq("t0k3n")))
                .thenReturn(new ResponseEntity("Activity successfully subscribed to", HttpStatus.valueOf(201)));
        this.mockMvc.perform(post("/profiles/1/subscriptions/activities/1").cookie(tokenCookie))
                .andExpect(status().is(201))
                .andExpect(content().string(containsString("Activity successfully subscribed to")));
    }

    /***
     * Users can't subscribe to an activity that they are already subscribed too
     */
    @Test
    public void subscribeToActivityThatUserIsAlreadySubscribedToo() throws Exception {
        final Cookie tokenCookie = new Cookie("s_id", "t0k3n");
        Session testSession = new Session("t0k3n");
        User testUser = new User("John", "Smith", "john@gmail.com", null,
                Gender.MALE, 2, "Password1");
        testUser.setUserId((long) 1);
        testUser.setPermissionLevel(0);
        testSession.setUser(testUser);

        Activity testActivity = new Activity("testActivity", "Used for testing", true,
                null, null);
        testActivity.setId((long) 1);

        when(service.subscribeToActivity(eq(1L), eq(1L), eq("t0k3n")))
                .thenReturn(new ResponseEntity("Activity successfully subscribed to", HttpStatus.valueOf(201)));
        this.mockMvc.perform(post("/profiles/1/subscriptions/activities/1").cookie(tokenCookie))
                .andExpect(status().is(201))
                .andExpect(content().string(containsString("Activity successfully subscribed to")));
        when(service.subscribeToActivity(eq(1L), eq(1L), eq("t0k3n")))
                .thenReturn(new ResponseEntity("Cannot subscribe to an activity that you have already subscribed to",
                        HttpStatus.valueOf(403)));
        this.mockMvc.perform(post("/profiles/1/subscriptions/activities/1").cookie(tokenCookie))
                .andExpect(status().is(403))
                .andExpect(content().string(containsString("Cannot subscribe to an activity that you have already subscribed to")));
    }
}
