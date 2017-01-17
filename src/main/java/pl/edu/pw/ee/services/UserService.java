package pl.edu.pw.ee.services;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

public interface UserService {
    ResponseEntity authenticate(JSONObject request);

    ResponseEntity details(JSONObject request);
}