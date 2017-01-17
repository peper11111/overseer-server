package pl.edu.pw.ee.services.impl;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.edu.pw.ee.entities.Details;
import pl.edu.pw.ee.entities.User;
import pl.edu.pw.ee.repositories.UserRepository;
import pl.edu.pw.ee.services.UserService;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public ResponseEntity authenticate(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByUsername(request.getString("username"));

        if (user == null) {
            response.put("error", "ERROR_USER_NOT_FOUND");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        if (!user.isActive()) {
            response.put("error", "ERROR_USER_INACTIVE");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        if (!(new BCryptPasswordEncoder().matches(request.getString("password"), user.getPassword()))) {
            response.put("error", "ERROR_INVALID_PASSWORD");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        String token = UUID.randomUUID().toString();
        user.setToken(token);
        userRepository.save(user);

        response.put("token", token);
        response.put("authenticated", true);

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity details(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        Details details = user.getDetails();

        response.put("name", details.getName());
        response.put("email", details.getEmail());
        response.put("mobile", details.getMobile());
        response.put("rank", details.getRank());
        response.put("team", details.getTeam());
        response.put("department", details.getDepartment());
        response.put("company", details.getCompany());
        response.put("avatar", Base64.encodeBase64String(details.getAvatar().getImage()));

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }
}