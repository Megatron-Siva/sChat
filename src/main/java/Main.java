import com.google.common.base.Strings;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

@WebServlet(name = "Main")
public class Main extends HttpServlet {
    Connection con;
    String status = "status", success = "success", fail = "fail", message = "meassage";
    PreparedStatement insertuser, deleteuser, selectuser, sendmessage, getallmessage, addcontact, updatereceivetime;

    public Main() {

        try {
            con = DatabaseConnection.initializeDatabase();
            insertuser = con.prepareStatement("insert into user (username,contact,name) values(?,?,?)");
            addcontact = con.prepareStatement("update user set contact=? where username=?");
            deleteuser = con.prepareStatement("DELETE FROM user WHERE username=(?)");
            selectuser = con.prepareStatement("select * from user where username=(?)");
            sendmessage = con.prepareStatement("insert into message (senderid,receiverid,message) values(?,?,?)");
            getallmessage = con.prepareStatement("select * from message where (senderid=? and receiverid=?) or (senderid=? and receiverid=?) order by sendtime");
            updatereceivetime = con.prepareStatement("update message set receivetime=current_timestamp where senderid=? and receiverid=?");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    JSONObject requesttojson(HttpServletRequest request) throws IOException {
        JSONObject payload = null;
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        JSONParser parser = new JSONParser();
        try {
            payload = (JSONObject) parser.parse(buffer.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return payload;
    }


    JSONObject jsonbuilder() {
        JSONObject j = new JSONObject();
        return j;
    }

    String existGetString(JSONObject j, String s) {
        if (j.containsKey(s)) {
            return String.valueOf(j.get(s));
        }
        return null;
    }

    //GET method
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        JSONObject payload = requesttojson(request);
        String pathInfo = request.getPathInfo();
        String out = "Sorry,Nothing here";
        response.setContentType("application/json");
        JSONObject jout = new JSONObject();
        JSONObject j = new JSONObject();

        try {
            if (pathInfo.equals("/contact")) {
                String username = existGetString(payload, "username");
                if (Strings.isNullOrEmpty(username)) {
                    jout.put(status, fail);
                    jout.put(message, "Enter valid username");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }
                selectuser.setString(1, username);
                ResultSet rs = selectuser.executeQuery();
                JSONObject contact = null;
                Boolean userexist = false;
                while (rs.next()) {
                    contact = (JSONObject) new JSONParser().parse((String) rs.getObject(3));
                    userexist = true;
                    break;
                }
                if (!userexist) {
                    jout.put(status, fail);
                    jout.put("message", "No such a username exist");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                } else {
                    JSONObject tj=new JSONObject();
                    JSONArray arr= (JSONArray) contact.get("contact");
                    for (int i=0;i< arr.size();i++) {
                        selectuser.setString(1, String.valueOf(arr.get(i)));
                        rs =selectuser.executeQuery();
                        rs.next();
                        tj.put(arr.get(i),rs.getString(5));

                    }
                    jout.put("contact", tj);
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }

            } else if (pathInfo.equals("/chat")) {
                String username = existGetString(payload, "username");
                String receiverUsername = existGetString(payload, "receiver-username");
                if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(receiverUsername)) {
                    jout.put(status, fail);
                    jout.put(message, "Enter valid username and receiver-username");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }
                if (!username.equals(receiverUsername)) {
                    selectuser.setString(1, username);
                    ResultSet rs = selectuser.executeQuery();
                    int senderid = 0, receiverid = 0;
                    String senderName, receiverName;
                    Boolean userexist = false;
                    while (rs.next()) {
                        senderid = rs.getInt(1);
                        userexist = true;
                        break;
                    }
                    if (!userexist) {
                        jout.put(status, fail);
                        jout.put("message", "No such a username exist");
                        response.getOutputStream().println(String.valueOf(jout));
                        return;
                    } else {
                        selectuser.setString(1, receiverUsername);
                        rs = selectuser.executeQuery();
                        userexist = false;
                        while (rs.next()) {
                            receiverid = rs.getInt(1);
                            userexist = true;
                            break;
                        }
                        if (!userexist) {
                            jout.put(status, fail);
                            jout.put("message", "No such a receiver-username exist");
                            response.getOutputStream().println(String.valueOf(jout));
                            return;
                        } else {

                            updatereceivetime.setInt(1, receiverid);
                            updatereceivetime.setInt(2, senderid);
                            updatereceivetime.executeUpdate();
                            getallmessage.setInt(1, senderid);
                            getallmessage.setInt(2, receiverid);
                            getallmessage.setInt(3, receiverid);
                            getallmessage.setInt(4, senderid);
                            rs = getallmessage.executeQuery();
                            HashMap<Integer, String> idname = new HashMap<>();
                            idname.put(senderid, username);
                            idname.put(receiverid, receiverUsername);
                            JSONArray marr = new JSONArray();
                            while (rs.next()) {
                                senderid = rs.getInt(2);
                                String message = rs.getString(3);
                                receiverid = rs.getInt(4);
                                String sendtime = rs.getString(5);
                                String receivetime = rs.getString(6);
                                JSONObject collect = new JSONObject();
                                collect.put("sender-username", idname.get(senderid));
                                collect.put("receiver-username", idname.get(receiverid));
                                collect.put("message", message);
                                if (receivetime == null) {
                                    collect.put("message-status", "Not received");
                                } else {
                                    collect.put("message-status", "received");
                                    collect.put("receive-time", receivetime);
                                }
                                JSONObject conatinarr = new JSONObject();
                                conatinarr.put(sendtime, collect);
                                marr.add(conatinarr);
                            }
                            jout.put("messages", marr);
                            response.getOutputStream().println(String.valueOf(jout));
                            return;
                        }
                    }
                } else {
                    jout.put(status, fail);
                    jout.put("message", "sender and receiver usernames are same");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            jout.put(status, fail);
            jout.put("error", e);
            response.getOutputStream().println(String.valueOf(jout));
            return;
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

    //  POST method
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JSONObject payload = requesttojson(request);
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        JSONObject jout = new JSONObject();
        try {
            if (pathInfo.equals("/user")) {
//                addUser
                String username = existGetString(payload, "username");
                String name = existGetString(payload, "name");
                if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(username)) {
                    jout.put(status, fail);
                    jout.put(message, "Enter valid name and username");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;

                }
                selectuser.setString(1, username);
                ResultSet rs = selectuser.executeQuery();
                Boolean userexist = false;
                while (rs.next()) {
                    userexist = true;
                    break;
                }
                if (!userexist) {
                    insertuser.setString(1, username);
                    JSONObject j = new JSONObject();
                    j.put("contact", new JSONArray());
                    insertuser.setObject(2, j.toString());
                    insertuser.setString(3, name);
                    insertuser.executeUpdate();
                    jout.put(status, success);
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                } else {
                    jout.put(status, fail);
                    jout.put("message", "username already exits.Choose a diffrent username");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }

            } else if (pathInfo.equals("/contact")) {
//                addContact
                String username = existGetString(payload, "username");
                String contactUsername = existGetString(payload, "contact-username");
                if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(contactUsername)) {
                    jout.put(status, fail);
                    jout.put(message, "Enter valid username and contact-username");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }
                JSONObject contact = null;
                if (!username.equals(contactUsername)) {
                    selectuser.setString(1, username);
                    ResultSet rs = selectuser.executeQuery();
                    Boolean userexist = false;
                    while (rs.next()) {
                        contact = (JSONObject) new JSONParser().parse((String) rs.getObject(3));
                        userexist = true;
                        break;
                    }
                    if (!userexist) {
                        jout.put(status, fail);
                        jout.put("message", "No such a username exist");
                        response.getOutputStream().println(String.valueOf(jout));
                        return;
                    } else {
                        selectuser.setString(1, contactUsername);
                        rs = selectuser.executeQuery();
                        userexist = false;
                        while (rs.next()) {
                            userexist = true;
                            break;
                        }
                        if (!userexist) {
                            jout.put(status, fail);
                            jout.put("message", "No such a contactname exist");
                            response.getOutputStream().println(String.valueOf(jout));
                            return;
                        } else {
                            JSONArray arr = (JSONArray) contact.get("contact");
                            if (!arr.contains(contactUsername)) {
                                arr.add(contactUsername);
                                contact.put("contact", arr);
                                addcontact.setObject(1, contact.toString());
                                addcontact.setString(2, username);
                                addcontact.executeUpdate();
                                jout.put(status, success);
                                response.getOutputStream().println(String.valueOf(jout));
                                return;
                            } else {
                                jout.put(status, fail);
                                jout.put("message", "Contact Name already exist");
                                response.getOutputStream().println(String.valueOf(jout));
                                return;
                            }
                        }

                    }
                } else {
                    jout.put(status, fail);
                    jout.put("message", "user and contact usernames are same");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }
            } else if (pathInfo.equals("/chat")) {
                String username = existGetString(payload, "username");
                String receiverUsername = existGetString(payload, "receiver-username");
                String message = existGetString(payload, "message");
                if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(receiverUsername) || Strings.isNullOrEmpty(message)) {
                    jout.put(status, fail);
                    jout.put(message, "Enter valid username and receiver-username");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }
                if (!username.equals(receiverUsername)) {
                    selectuser.setString(1, username);
                    ResultSet rs = selectuser.executeQuery();
                    int senderid = 0, receiverid = 0;
                    Boolean userexist = false;
                    while (rs.next()) {
                        senderid = rs.getInt(1);
                        userexist = true;
                        break;
                    }
                    if (!userexist) {
                        jout.put(status, fail);
                        jout.put("message", "No such a username exist");
                        response.getOutputStream().println(String.valueOf(jout));
                        return;
                    } else {
                        selectuser.setString(1, receiverUsername);
                        rs = selectuser.executeQuery();
                        userexist = false;
                        while (rs.next()) {
                            receiverid = rs.getInt(1);
                            userexist = true;
                            break;
                        }
                        if (!userexist) {
                            jout.put(status, fail);
                            jout.put("message", "No such a receiver exist");
                            response.getOutputStream().println(String.valueOf(jout));
                            return;
                        } else {
                            sendmessage.setInt(1, senderid);
                            sendmessage.setInt(2, receiverid);
                            sendmessage.setString(3, message);
                            sendmessage.executeUpdate();
                            jout.put(status, success);
                            response.getOutputStream().println(String.valueOf(jout));
                            return;
                        }
                    }
                } else {
                    jout.put(status, fail);
                    jout.put("message", "sender and receiver usernames are same");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }
            } else {
                response.setStatus(404);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            jout.put(status, fail);
            jout.put("error", e);

        } catch (ParseException e) {
            jout.put(status, fail);
            jout.put("message", "Provide the data correctly");
            e.printStackTrace();
        }

    }

    //PUT METHOD
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JSONObject payload = requesttojson(request);
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        String out = "Sorry,Nothing here";
        JSONObject jout = new JSONObject();
        try {


            if (pathInfo == null || pathInfo.equals("/")) {
                out = "Welcome Home";

            } else if (pathInfo.equals("/user")) {


            } else if (pathInfo.equals("/chat")) {
                out = "chat";
            }


        } catch (Exception e) {
            e.printStackTrace();
            jout.put(status, fail);
            jout.put("error", e);


        }
        response.getOutputStream().println(String.valueOf(jout));

    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JSONObject payload = requesttojson(request);
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        String out = "Sorry,Nothing here";
        JSONObject jout = new JSONObject();
        try {
            if (pathInfo.equals("/user")) {
//                deleteUser
                String username = existGetString(payload, "username");
                if (Strings.isNullOrEmpty(username)) {
                    jout.put(status, fail);
                    jout.put(message, "Enter valid username");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;

                }
                selectuser.setString(1, username);
                ResultSet rs = selectuser.executeQuery();
                Boolean userexist = false;
                while (rs.next()) {
                    userexist = true;
                    break;
                }
                if (userexist) {
                    deleteuser.setString(1, username);
                    deleteuser.executeUpdate();
                    jout.put(status, success);
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                } else {
                    jout.put(status, fail);
                    jout.put("message", "No such a username");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }
            } else if (pathInfo.equals("/contact")) {
//                deleteContact
                String username = String.valueOf(payload.get("username"));
                String contactUsername = String.valueOf(payload.get("contact-username"));
                JSONObject contact = null;
                if (!username.equals(contactUsername)) {
                    selectuser.setString(1, username);
                    ResultSet rs = selectuser.executeQuery();
                    Boolean userexist = false;
                    while (rs.next()) {
                        contact = (JSONObject) new JSONParser().parse((String) rs.getObject(3));
                        userexist = true;
                        break;
                    }
                    if (!userexist) {
                        jout.put(status, fail);
                        jout.put("message", "No such a username exist");
                        response.getOutputStream().println(String.valueOf(jout));
                        return;
                    } else {
                        selectuser.setString(1, contactUsername);
                        rs = selectuser.executeQuery();
                        userexist = false;
                        while (rs.next()) {
                            userexist = true;
                            break;
                        }
                        if (!userexist) {
                            jout.put(status, fail);
                            jout.put("message", "No such a contactname exist");
                            response.getOutputStream().println(String.valueOf(jout));
                            return;
                        } else {
                            JSONArray arr = (JSONArray) contact.get("contact");
                            if (arr.contains(contactUsername)) {
                                arr.remove(contactUsername);
                                contact.put("contact", arr);
                                addcontact.setObject(1, contact.toString());
                                addcontact.setString(2, username);
                                addcontact.executeUpdate();
                                jout.put(status, success);
                                response.getOutputStream().println(String.valueOf(jout));
                                return;
                            } else {
                                jout.put(status, fail);
                                jout.put("message", "No such a contactname exist in your contacts");
                                response.getOutputStream().println(String.valueOf(jout));
                                return;
                            }
                        }

                    }
                } else {
                    jout.put(status, fail);
                    jout.put("message", "user and contact usernames are same");
                    response.getOutputStream().println(String.valueOf(jout));
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            jout.put(status, fail);
            jout.put("error", e);
            response.getOutputStream().println(String.valueOf(jout));
            return;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
