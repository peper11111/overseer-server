package pl.edu.pw.ee.services;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

public interface IndexService {
    ResponseEntity authenticate(JSONObject request);

    ResponseEntity profile(JSONObject request);

    ResponseEntity password(JSONObject request);

    ResponseEntity subordinates(JSONObject request);

    ResponseEntity location(JSONObject request);

    ResponseEntity start(JSONObject request);

    ResponseEntity stop(JSONObject request);

    ResponseEntity statistics(JSONObject request);

    ResponseEntity history(JSONObject request);


}