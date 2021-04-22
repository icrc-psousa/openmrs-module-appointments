package org.openmrs.module.appointments.telehealth;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class TeleHealthServiceImpl implements TeleHealthService {

    private String token;

    @Autowired
    RestTemplate restTemplate;

    @Override
    public Boolean authenticate() {
        String uri = "https://dev-medecin-hug-at-home.oniabsis.com/api/v1/login-local";
        User user = new User();
        user.setEmail("cicr@test.org");
        user.setPassword("123456");
        UserAuthResponse userAuthResponse = restTemplate.postForObject(uri, user, UserAuthResponse.class);
        token = userAuthResponse.getUser().getToken();
        return true;
    }

    @Override
    public Invite invite(InvitationRequest invitationRequest) {

        if (authenticate()) {
            String uri = "https://dev-medecin-hug-at-home.oniabsis.com/api/v1/invite/";

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", token);
            HttpEntity<InvitationRequest> request = new HttpEntity<>(invitationRequest, headers);

            InvitationResponse response = null;
            if(invitationRequest.getId()==null){
                response = restTemplate.postForObject(uri, request, InvitationResponse.class);
                return response.getInvite();
            }else{
                uri += invitationRequest.getId();
                updateAtHomeEncounter(uri, invitationRequest);
            }
        }
        return null;
    }

    @Override
    public void delete(String invitationID) {
        if (authenticate()) {
            String uri = "https://dev-medecin-hug-at-home.oniabsis.com/api/v1/invite/" + invitationID;

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", token);
            HttpEntity<InvitationRequest> request = new HttpEntity<>(headers);
            restTemplate.exchange(uri, HttpMethod.DELETE, request, Void.class, 1);
        }
    }

    private void updateAtHomeEncounter(String uri, InvitationRequest invitationRequest) {

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        org.apache.http.HttpEntity httpEntity = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPatch httpPatch = new HttpPatch(uri);
            httpPatch.setHeader("x-access-token", token);
            httpPatch.setHeader("Content-Type", "application/json");

            String content = ow.writeValueAsString(invitationRequest);
            if (null != content) {
                httpEntity = new ByteArrayEntity(content.getBytes("UTF-8"));
                httpPatch.setEntity(httpEntity);
            }
            HttpResponse httpResponse = httpClient.execute(httpPatch);
            httpResponse.getStatusLine().getStatusCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
