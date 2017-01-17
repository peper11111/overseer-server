package pl.edu.pw.ee.controllers;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.pw.ee.entities.Avatar;
import pl.edu.pw.ee.repositories.AvatarRepository;
import pl.edu.pw.ee.services.UserService;

import java.io.File;
import java.io.FileInputStream;

@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/user/authenticate", method = RequestMethod.POST)
    public ResponseEntity authenticate(@RequestBody String request) {
        return userService.authenticate(new JSONObject(request));
    }

    @RequestMapping(value = "/user/details", method = RequestMethod.POST)
    public ResponseEntity details(@RequestBody String request) {
        return userService.details(new JSONObject(request));
    }

    @Autowired
    private AvatarRepository avatarRepository;

    @RequestMapping(value = "/image", method = RequestMethod.GET)
    public void image() {
        File file = new File("/home/peper11111/Pulpit/avatar.png");
        byte[] image = new byte[(int) file.length()];

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(image);
            fileInputStream.close();
            System.out.println(image.length);

            Avatar avatar = new Avatar();
            avatar.setImage(image);
            avatarRepository.save(avatar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}