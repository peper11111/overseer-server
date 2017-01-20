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
        for (User subordinate : userRepository.findBySupervisor(user)) {
            Details details = subordinate.getDetails();
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("id", subordinate.getId());
            jsonObject.put("name", getName(details));
            jsonObject.put("email", details.getEmail());
            jsonObject.put("avatar", Base64.encodeBase64String(details.getAvatar().getImage()));

            array.put(jsonObject);

            getSubordinates(array, subordinate);
        }
    }

    @Override
    public ResponseEntity details(JSONObject request) {
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        JSONObject profile = new JSONObject();
        User subordinate = userRepository.findOne(request.getLong("id"));
        Details details = subordinate.getDetails();
        profile.put("phone", details.getPhone());
        profile.put("mobile", details.getMobile());
        profile.put("address", details.getAddress());
        profile.put("zip", details.getZip());
        profile.put("city", details.getCity());
        profile.put("joined", new SimpleDateFormat("yyyy-MM-dd").format(new Date(details.getJoined())));
        profile.put("rank", details.getRank());
        profile.put("team", details.getTeam());
        profile.put("department", details.getDepartment());
        profile.put("company", details.getCompany());
        if (subordinate.getSupervisor() != null)
            profile.put("supervisor", getName(subordinate.getSupervisor().getDetails()));
        else
            profile.put("supervisor", "-");

        response.put("profile", profile);


        JSONObject current = new JSONObject();
        JSONObject previous = new JSONObject();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date now = calendar.getTime();

        JSONObject currentMonth = new JSONObject();
        getMonth(calendar, currentMonth, subordinate);
        current.put("month", currentMonth);

        calendar.setTime(now);
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
        JSONObject previousMonth = new JSONObject();
        getMonth(calendar, previousMonth, subordinate);
        previous.put("month", previousMonth);

        JSONObject workTime = new JSONObject();
        workTime.put("current", current);
        workTime.put("previous", previous);

        response.put("worktime", workTime);

        ArrayList locations = new ArrayList();
        for (Location location : locationRepository.findByUser(subordinate)) {
            JSONObject loc = new JSONObject();
            loc.put("date", location.getDate());
            loc.put("latitude", location.getLatitude());
            loc.put("longitude", location.getLongitude());
            locations.add(loc);
        }

        Collections.sort(locations, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                long d1 = ((JSONObject) o1).getLong("date");
                long d2 = ((JSONObject) o2).getLong("date");
                if (d1 < d2)
                    return 1;
                if (d1 > d2)
                    return -1;
                return 0;
            }
        });

        response.put("locations", locations);

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
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
        //TODO Naprawic sytuacje rekordów z wieloma nulami na stopie.
        JSONObject response = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        Calendar calendar = Calendar.getInstance();

        for (WorkTime workTime : workTimeRepository.findByUser(user)) {
            long start = workTime.getStart();
            long stop = request.getLong("stop");

            calendar.setTimeInMillis(stop);
            int stopDay = calendar.get(Calendar.DATE);
            calendar.setTimeInMillis(start);
            int startDay = calendar.get(Calendar.DATE);

            if (startDay == stopDay) {
                workTime.setStop(stop);
                workTime.setSummary(stop - start);
                workTimeRepository.save(workTime);
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                calendar.add(Calendar.DATE, 1);
                long tmpStop = calendar.getTime().getTime();
                workTime.setStop(tmpStop);
                workTime.setSummary(tmpStop - start);
                workTimeRepository.save(workTime);

                WorkTime newWorkTime = new WorkTime();
                newWorkTime.setStart(tmpStop);
                newWorkTime.setStop(stop);
                newWorkTime.setSummary(stop - tmpStop);
                newWorkTime.setUser(workTime.getUser());
                workTimeRepository.save(newWorkTime);
            }
            break;
        }

        response.put("success", "SUCCESS_OK");

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @Override
    public ResponseEntity statistics(JSONObject request) {
        JSONObject response = new JSONObject();
        JSONObject current = new JSONObject();
        JSONObject previous = new JSONObject();

        User user = userRepository.findByToken(request.getString("token"));
        if (user == null || !user.isActive()) {
            response.put("error", "AUTHENTICATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date now = calendar.getTime();

        JSONObject currentWeek = new JSONObject();
        getWeek(calendar, currentWeek, user);
        current.put("week", currentWeek);

        calendar.setTime(now);
        calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 1);
        JSONObject previousWeek = new JSONObject();
        getWeek(calendar, previousWeek, user);
        previous.put("week", previousWeek);

        calendar.setTime(now);
        JSONObject currentMonth = new JSONObject();
        getMonth(calendar, currentMonth, user);
        current.put("month", currentMonth);

        calendar.setTime(now);
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
        JSONObject previousMonth = new JSONObject();
        getMonth(calendar, previousMonth, user);
        previous.put("month", previousMonth);

        calendar.setTime(now);
        calendar.set(Calendar.DATE, 1);
        JSONObject currentYear = new JSONObject();
        getYear(calendar, currentYear, user);
        current.put("year", currentYear);

        calendar.setTime(now);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
        JSONObject previousYear = new JSONObject();
        getYear(calendar, previousYear, user);
        previous.put("year", previousYear);

        response.put("current", current);
        response.put("previous", previous);

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    private void getWeek(Calendar calendar, JSONObject week, User user) {
        for (int i = calendar.getActualMinimum(Calendar.DAY_OF_WEEK); i <= calendar.getActualMaximum(Calendar.DAY_OF_WEEK); i++) {
            calendar.set(Calendar.DAY_OF_WEEK, i);
            Date start = calendar.getTime();

            Date stop = new Date(start.getTime() + 86400000);

            long summary = 0;
            for (WorkTime worktime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += worktime.getSummary();
            week.put("" + (i == 1 ? 7 : i - 1), summary);
        }
    }

    private void getMonth(Calendar calendar, JSONObject month, User user) {
        for (int i = calendar.getActualMinimum(Calendar.DATE); i <= calendar.getActualMaximum(Calendar.DATE); i++) {
            calendar.set(Calendar.DATE, i);
            Date start = calendar.getTime();

            Date stop = new Date(start.getTime() + 86400000);

            long summary = 0;
            for (WorkTime worktime : workTimeRepository.findByInterval(start.getTime(), stop.getTime(), user))
                summary += worktime.getSummary();
            month.put("" + i, summary);
        }
    }

    private void getYear(Calendar calendar, JSONObject year, User user) {
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
    }
}