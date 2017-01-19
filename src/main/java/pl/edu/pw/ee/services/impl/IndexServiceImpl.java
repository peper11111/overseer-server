package pl.edu.pw.ee.services.impl;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.edu.pw.ee.entities.Details;
import pl.edu.pw.ee.entities.Location;
import pl.edu.pw.ee.entities.User;
import pl.edu.pw.ee.entities.WorkTime;
import pl.edu.pw.ee.repositories.LocationRepository;
import pl.edu.pw.ee.repositories.UserRepository;
import pl.edu.pw.ee.repositories.WorkTimeRepository;
import pl.edu.pw.ee.services.IndexService;

import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
public class IndexServiceImpl implements IndexService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkTimeRepository workTimeRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Override
    public ResponseEntity authenticate(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByUsername(request.getString("username"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        if (!(new BCryptPasswordEncoder().matches(request.getString("password"), user.getPassword()))) {
            response.put("error", "PASSWORD_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        String token = UUID.randomUUID().toString();
        user.setToken(token);
        userRepository.save(user);

        Details details = user.getDetails();
        response.put("token", token);
        response.put("name", getName(details));
        response.put("email", details.getEmail());
        response.put("avatar", Base64.encodeBase64String(details.getAvatar().getImage()));

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    private String getName(Details details) {
        String name = details.getFirstName() + " ";
        if (details.getMiddleName() != null)
            name += details.getMiddleName() + " ";
        name += details.getLastName();
        return name;
    }

    @Override
    public ResponseEntity profile(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        Details details = user.getDetails();
        response.put("phone", details.getPhone());
        response.put("mobile", details.getMobile());
        response.put("address", details.getAddress());
        response.put("zip", details.getZip());
        response.put("city", details.getCity());
        response.put("joined", new SimpleDateFormat("yyyy-MM-dd").format(new Date(details.getJoined())));
        response.put("rank", details.getRank());
        response.put("team", details.getTeam());
        response.put("department", details.getDepartment());
        response.put("company", details.getCompany());
        if (user.getSupervisor() != null)
            response.put("supervisor", getName(user.getSupervisor().getDetails()));
        else
            response.put("supervisor", "-");

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity password(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        user.setPassword(new BCryptPasswordEncoder().encode(request.getString("password")));
        userRepository.save(user);

        response.put("success", "SUCCESS_OK");

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity subordinates(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        JSONArray array = new JSONArray();
        getSubordinates(array, user);
        response.put("subordinates", array);

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    private void getSubordinates(JSONArray array, User user) {
        for (User subordinate : userRepository.findBySupervisor(user.getId())) {
            Details details = subordinate.getDetails();
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("id", subordinate.getId());
            jsonObject.put("name", getName(details));
            jsonObject.put("email", details.getEmail());
            jsonObject.put("avatar", Base64.encodeBase64String(details.getAvatar().getImage()));

            array.put(jsonObject);

            if (subordinate.getSupervisor() != null)
                getSubordinates(array, subordinate.getSupervisor());
        }
    }

    @Override
    public ResponseEntity location(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        Location location = new Location();
        location.setLatitude(request.getDouble("latitude"));
        location.setLongitude(request.getDouble("longitude"));
        location.setDate(request.getLong("date"));
        location.setUser(user);
        locationRepository.save(location);

        response.put("success", "SUCCESS_OK");

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity start(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        WorkTime worktime = new WorkTime();
        worktime.setStart(request.getLong("start"));
        worktime.setUser(user);
        workTimeRepository.save(worktime);

        response.put("success", "SUCCESS_OK");

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity stop(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        for (WorkTime workTime : workTimeRepository.findByUser(user)) {
            long start = workTime.getStart();
            long stop = request.getLong("stop");

            workTime.setStop(stop);
            workTime.setSummary(stop - start);
            workTimeRepository.save(workTime);

            break;
        }

        response.put("success", "SUCCESS_OK");

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity statistics(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null) {
            response.put("error", "ERROR_USER_TOKEN");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date now = calendar.getTime();

        JSONObject week = new JSONObject();
        for (int i = calendar.getActualMinimum(Calendar.DAY_OF_WEEK); i <= calendar.getActualMaximum(Calendar.DAY_OF_WEEK); i++) {
            calendar.set(Calendar.DAY_OF_WEEK, i);
            Date start = calendar.getTime();

            Date stop = new Date(start.getTime() + 86400000);

            long summary = 0;
            for (WorkTime worktime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += worktime.getSummary();
            week.put("" + (i == 1 ? 7 : i - 1), summary);
        }
        response.put("week", week);

        calendar.setTime(now);

        JSONObject month = new JSONObject();
        for (int i = calendar.getActualMinimum(Calendar.DATE); i <= calendar.getActualMaximum(Calendar.DATE); i++) {
            calendar.set(Calendar.DATE, i);
            Date start = calendar.getTime();

            Date stop = new Date(start.getTime() + 86400000);

            long summary = 0;
            for (WorkTime worktime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += worktime.getSummary();
            month.put("" + i, summary);
        }
        response.put("month", month);

        calendar.setTime(now);
        calendar.set(Calendar.DATE, 1);

        JSONObject year = new JSONObject();
        for (int i = calendar.getActualMinimum(Calendar.MONTH); i <= calendar.getActualMaximum(Calendar.MONTH); i++) {
            calendar.set(Calendar.MONTH, i);
            Date start = calendar.getTime();

            calendar.set(Calendar.MONTH, i + 1);
            Date stop = calendar.getTime();

            long summary = 0;
            for (WorkTime worktime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += worktime.getSummary();
            year.put("" + (i + 1), summary);
        }
        response.put("year", year);

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity history(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null) {
            response.put("error", "ERROR_USER_TOKEN");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date now = calendar.getTime();
        calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 1);

        JSONObject week = new JSONObject();
        for (int i = calendar.getActualMinimum(Calendar.DAY_OF_WEEK); i <= calendar.getActualMaximum(Calendar.DAY_OF_WEEK); i++) {
            calendar.set(Calendar.DAY_OF_WEEK, i);
            Date start = calendar.getTime();

            Date stop = new Date(start.getTime() + 86400000);

            long summary = 0;
            for (WorkTime worktime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += worktime.getSummary();
            week.put("" + (i == 1 ? 7 : i - 1), summary);
        }
        response.put("week", week);

        calendar.setTime(now);
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);

        JSONObject month = new JSONObject();
        for (int i = calendar.getActualMinimum(Calendar.DATE); i <= calendar.getActualMaximum(Calendar.DATE); i++) {
            calendar.set(Calendar.DATE, i);
            Date start = calendar.getTime();

            Date stop = new Date(start.getTime() + 86400000);

            long summary = 0;
            for (WorkTime worktime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += worktime.getSummary();
            month.put("" + i, summary);
        }
        response.put("month", month);

        calendar.setTime(now);
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
        calendar.set(Calendar.DATE, 1);

        JSONObject year = new JSONObject();
        for (int i = calendar.getActualMinimum(Calendar.MONTH); i <= calendar.getActualMaximum(Calendar.MONTH); i++) {
            calendar.set(Calendar.MONTH, i);
            Date start = calendar.getTime();

            calendar.set(Calendar.MONTH, i + 1);
            Date stop = calendar.getTime();

            long summary = 0;
            for (WorkTime worktime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += worktime.getSummary();
            year.put("" + (i + 1), summary);
        }
        response.put("year", year);

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }


}