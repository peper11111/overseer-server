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
import pl.edu.pw.ee.entities.WorkTime;
import pl.edu.pw.ee.repositories.UserRepository;
import pl.edu.pw.ee.repositories.WorkTimeRepository;
import pl.edu.pw.ee.services.IndexService;

import javax.transaction.Transactional;
import java.util.*;

@Service
@Transactional
public class IndexServiceImpl implements IndexService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkTimeRepository workTimeRepository;

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

        Details details = user.getDetails();

        response.put("token", token);
        response.put("name", details.getName());
        response.put("email", details.getEmail());
        response.put("mobile", details.getMobile());
        response.put("rank", details.getRank());
        response.put("team", details.getTeam());
        response.put("department", details.getDepartment());
        response.put("company", details.getCompany());
        response.put("avatar", Base64.encodeBase64String(details.getAvatar().getImage()));

        if (user.getSupervisor() == null)
            response.put("supervisor", "-");
        else
            response.put("supervisor", user.getSupervisor().getDetails().getName());

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity start(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null) {
            response.put("error", "ERROR_USER_TOKEN");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        WorkTime workTime = new WorkTime();
        workTime.setStart(request.getLong("start"));
        workTime.setUser(user);

        workTimeRepository.save(workTime);

        response.put("success", "SUCCESS_OK");
        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity stop(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null) {
            response.put("error", "ERROR_USER_TOKEN");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        WorkTime workTime = workTimeRepository.findByUser(user);

        long start = workTime.getStart();
        long stop = request.getLong("stop");

        workTime.setStop(stop);
        workTime.setSummary(stop - start);

        workTimeRepository.save(workTime);

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
            for (WorkTime workTime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += workTime.getSummary();
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
            for (WorkTime workTime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += workTime.getSummary();
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
            for (WorkTime workTime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += workTime.getSummary();
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
            for (WorkTime workTime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += workTime.getSummary();
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
            for (WorkTime workTime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += workTime.getSummary();
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
            for (WorkTime workTime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += workTime.getSummary();
            year.put("" + (i + 1), summary);
        }
        response.put("year", year);

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }
}