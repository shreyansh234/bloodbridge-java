package com.bloodbridge;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class BloodBridgeFlowTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired AccountRepository accounts;
 @Test void accountAndDonorLifecycleProtectsContactDetails() throws Exception {
  mvc.perform(get("/api/session")).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("java")).andExpect(jsonPath("$.csrf").isString());
  String account="{\"name\":\"Test Person\",\"email\":\"test@example.test\",\"password\":\"TestPassword123!\"}";
  mvc.perform(post("/api/auth/register").contentType("application/json").content(account)).andExpect(status().isForbidden());
  var registered=mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(account)).andExpect(status().isCreated()).andReturn();
  MockHttpSession session=(MockHttpSession)registered.getRequest().getSession(false);assertNotNull(session);
  assertNotEquals("TestPassword123!",accounts.findByEmail("test@example.test").orElseThrow().passwordHash);
  mvc.perform(get("/api/session").session(session)).andExpect(jsonPath("$.user.email").value("test@example.test"));
  String profile="{\"name\":\"Test Person\",\"bloodGroup\":\"O+\",\"city\":\"Thane\",\"area\":\"Kopri\",\"phone\":\"9000000000\",\"available\":true,\"consent\":true}";
  var saved=mvc.perform(put("/api/profile").session(session).with(csrf()).contentType("application/json").content(profile)).andExpect(status().isOk()).andReturn();
  String id=json.readTree(saved.getResponse().getContentAsString()).path("profile").path("id").asText();
  mvc.perform(get("/api/donors").param("group","O+").param("city","THANE")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.donors[0].phone").doesNotExist()).andExpect(jsonPath("$.donors[0].account").doesNotExist());
  mvc.perform(get("/api/donors/"+id+"/contact")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/donors/"+id+"/contact").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("9000000000"));
  mvc.perform(patch("/api/profile/availability").session(session).with(csrf()).contentType("application/json").content("{\"available\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/donors/"+id+"/contact").session(session)).andExpect(status().isNotFound());
  mvc.perform(get("/api/donors").param("available","true")).andExpect(jsonPath("$.total").value(0));
  mvc.perform(get("/api/donors").param("available","false")).andExpect(jsonPath("$.total").value(1));
  mvc.perform(delete("/api/profile").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/donors")).andExpect(jsonPath("$.total").value(0));
  mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"test@example.test\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"test@example.test\",\"password\":\"TestPassword123!\"}")).andExpect(status().isOk());
 }
}
