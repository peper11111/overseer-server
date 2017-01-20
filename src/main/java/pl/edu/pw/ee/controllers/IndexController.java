package pl.edu.pw.ee.controllers;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.pw.ee.entities.Avatar;
import pl.edu.pw.ee.repositories.AvatarRepository;
import pl.edu.pw.ee.services.IndexService;

import java.io.File;
import java.io.FileInputStream;

@RestController
public class IndexController {
    @Autowired
    private IndexService indexService;

    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity authenticate(@RequestBody String request) {
        return indexService.authenticate(new JSONObject(request));
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public ResponseEntity profile(@RequestBody String request) {
        return indexService.profile(new JSONObject(request));
    }

    @RequestMapping(value = "/subordinates", method = RequestMethod.POST)
    public ResponseEntity subordinates(@RequestBody String request) {
        return indexService.subordinates(new JSONObject(request));
    }

    @RequestMapping(value = "/details", method = RequestMethod.POST)
    public ResponseEntity details(@RequestBody String request) {
        return indexService.details(new JSONObject(request));
    }

    @RequestMapping(value = "/location", method = RequestMethod.POST)
    public ResponseEntity location(@RequestBody String request) {
        return indexService.location(new JSONObject(request));
    }

    @RequestMapping(value = "/start", method = RequestMethod.POST)
    public ResponseEntity start(@RequestBody String request) {
        return indexService.start(new JSONObject(request));
    }

    @RequestMapping(value = "/stop", method = RequestMethod.POST)
    public ResponseEntity stop(@RequestBody String request) {
        return indexService.stop(new JSONObject(request));
    }

    @RequestMapping(value = "/statistics", method = RequestMethod.POST)
    public ResponseEntity statistics(@RequestBody String request) {
        return indexService.statistics(new JSONObject(request));
    }

    @Autowired
    private AvatarRepository avatarRepository;

    @RequestMapping(value = "/avatar/{name}", method = RequestMethod.GET)
    public void avatar(@PathVariable String name) {
        File file = new File("/home/peper11111/Pulpit/" + name + ".png");
        byte[] image = new byte[(int) file.length()];

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(image);
            fileInputStream.close();

            Avatar avatar = new Avatar();
            avatar.setImage(image);
            avatarRepository.save(avatar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}