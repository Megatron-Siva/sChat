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
    String status="status",succcess="success",fail="fail";
    PreparedStatement insertuser,deleteuser,selectuser,sendmessage,getallmessage,addcontact;

    public Main(){

        try {
            con= DatabaseConnection.initializeDatabase();
            insertuser= con.prepareStatement("insert into user (username,contact) values(?,?)");
            addcontact=con.prepareStatement("update user set contact=? where username=?");
            deleteuser=con.prepareStatement("DELETE FROM user WHERE username=(?)");
            selectuser=con.prepareStatement("select * from user where username=(?)");
            sendmessage=con.prepareStatement("insert into message (senderid,receiverid,message) values(?,?,?)");
            getallmessage=con.prepareStatement("select * from message where (senderid=? and receiverid=?) or (senderid=? and receiverid=?) order by datetime");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    //PreparedStatement st = con.prepareStatement("insert into demo values(?, ?)");
    JSONObject requesttojson(HttpServletRequest request) throws IOException {
        JSONObject payload=null;
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


    JSONObject jsonbuilder(){
        JSONObject j=new JSONObject();
        return j;
    }

    //GET method
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        JSONObject payload= requesttojson(request);
        String pathInfo = request.getPathInfo();
        String out="Sorry,Nothing here";
        response.setContentType("application/json");
        JSONObject jout = new JSONObject();
        JSONObject j=new JSONObject();

        try {

            if (pathInfo == null || pathInfo.equals("/")) {
                out="Welcom Home";

            } else if (pathInfo.equals("/contact")) {
                if (payload.get("action").equals("getallcontact")) {
                    String username = String.valueOf(payload.get("username"));
                    selectuser.setString(1, username);
                    ResultSet rs = selectuser.executeQuery();
                    JSONObject contact=null;
                    Boolean userexist = false;
                    while (rs.next()) {
                        contact = (JSONObject) new JSONParser().parse((String) rs.getObject(3));
                        userexist = true;
                        break;
                    }
                    if (!userexist) {
                        jout.put(status, fail);
                        jout.put("reason", "No such a username exist");
                    } else {
                        jout.put("contact",contact.get("contact"));
                    }

                }
            } else if (pathInfo.equals("/chat")) {
                if (payload.get("action").equals("getallmessage")) {
                    String username = String.valueOf(payload.get("username"));
                    String receivername = String.valueOf(payload.get("receivername"));
                    if (!username.equals(receivername)) {
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
                            jout.put("reason", "No such a username exist");
                        } else {
                            selectuser.setString(1, receivername);
                            rs = selectuser.executeQuery();
                            userexist = false;
                            while (rs.next()) {
                                receiverid = rs.getInt(1);
                                userexist = true;
                                break;
                            }
                            if (!userexist) {
                                jout.put(status, fail);
                                jout.put("reason", "No such a receiver exist");
                            } else {

                                getallmessage.setInt(1,senderid);
                                getallmessage.setInt(2,receiverid);
                                getallmessage.setInt(3,receiverid);
                                getallmessage.setInt(4,senderid);
                                rs=getallmessage.executeQuery();
                                HashMap<Integer,String> idname=new HashMap<>();
                                idname.put(senderid,username);
                                idname.put(receiverid,receivername);
                                JSONArray marr=new JSONArray();
                                while (rs.next()) {
                                    senderid = rs.getInt(2);
                                    String message=rs.getString(3);
                                    receiverid=rs.getInt(4);
                                    String datetime=rs.getString(5);
                                    JSONArray arr=new JSONArray();
                                    arr.add(idname.get(senderid));
                                    arr.add(idname.get(receiverid));
                                    arr.add(message);
                                    JSONObject conatinarr=new JSONObject();
                                    conatinarr.put(datetime,arr);
                                    marr.add(conatinarr);
                                }
                                jout.put("messages",marr);

                            }
                        }
                    } else {
                        jout.put(status, fail);
                        jout.put("reason", "sender and receiver usernames are same");
                    }

                }
            }
        }  catch (SQLException e) {
            e.printStackTrace();
            jout.put(status,fail);
            jout.put("error",e);


        } catch (ParseException e) {
            e.printStackTrace();
        }
        response.getOutputStream().println(String.valueOf(jout));


    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JSONObject payload = requesttojson(request);
        String pathInfo = request.getPathInfo();
        String out="Sorry,Nothing here";
        response.setContentType("application/json");
        JSONObject jout = new JSONObject();
        try {



            if (pathInfo == null || pathInfo.equals("/")) {
                out = "Welcome Home";

            } else if (pathInfo.equals("/user")) {
                if (payload.get("action").equals("adduser")) {
                    String username=String.valueOf(payload.get("username"));
                    selectuser.setString(1,username);
                    ResultSet rs=selectuser.executeQuery();
                    Boolean userexist=false;
                    while(rs.next()){
                        userexist=true;
                        break;
                    }
                    if(!userexist) {
                        insertuser.setString(1, username);
                        JSONObject j=new JSONObject();
                        j.put("contact", new JSONArray());
                        insertuser.setObject(2, j.toString());
                        insertuser.executeUpdate();
                        jout.put(status, succcess);
                    }
                    else {
                        jout.put(status,fail);
                        jout.put("reason","username already exits.Choose a diffrent username");
                    }

                }

            } else if (pathInfo.equals("/contact")) {
                if (payload.get("action").equals("addcontact")) {
                    String username = String.valueOf(payload.get("username"));
                    String contactname = String.valueOf(payload.get("contactname"));
                    JSONObject contact=null;
                    if (!username.equals(contactname)){
                        selectuser.setString(1,username);
                        ResultSet rs=selectuser.executeQuery();
                        Boolean userexist = false;
                        while (rs.next()) {
                            contact = (JSONObject) new JSONParser().parse((String) rs.getObject(3));
                            userexist = true;
                            break;
                        }
                        if (!userexist) {
                            jout.put(status, fail);
                            jout.put("reason", "No such a username exist");
                        } else {
                            selectuser.setString(1, contactname);
                            rs = selectuser.executeQuery();
                            userexist = false;
                            while (rs.next()) {
                                userexist = true;
                                break;
                            }
                            if (!userexist) {
                                jout.put(status, fail);
                                jout.put("reason", "No such a contactname exist");
                            } else{
                            JSONArray arr = (JSONArray) contact.get("contact");
                            if (!arr.contains(contactname)) {
                                arr.add(contactname);
                                contact.put("contact", arr);
                                addcontact.setObject(1, contact.toString());
                                addcontact.setString(2, username);
                                addcontact.executeUpdate();
                                jout.put(status, succcess);
                            } else {
                                jout.put(status, fail);
                                jout.put("reason", "Contact Name already exist");
                            }
                        }

                        }
                    }
                    else{
                        jout.put(status,fail);
                        jout.put("reason","user and contact usernames are same");
                    }

                }

            }
            else if (pathInfo.equals("/chat")){
                if (payload.get("action").equals("sendmessage")) {
                    String username = String.valueOf(payload.get("username"));
                    String receivername = String.valueOf(payload.get("receivername"));
                    String message = String.valueOf(payload.get("message"));
                    if (!username.equals(receivername)){
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
                        jout.put("reason", "No such a username exist");
                    } else {
                        selectuser.setString(1, receivername);
                        rs = selectuser.executeQuery();
                        userexist = false;
                        while (rs.next()) {
                            receiverid = rs.getInt(1);
                            userexist = true;
                            break;
                        }
                        if (!userexist) {
                            jout.put(status, fail);
                            jout.put("reason", "No such a receiver exist");
                        } else {
                            sendmessage.setInt(1, senderid);
                            sendmessage.setInt(2, receiverid);
                            sendmessage.setString(3, message);
                            sendmessage.executeUpdate();
                            jout.put(status, succcess);
                        }
                    }
                }
                    else{
                        jout.put(status,fail);
                        jout.put("reason","sender and receiver usernames are same");
                    }

                }
            }





        } catch (SQLException e) {
            e.printStackTrace();
            jout.put(status,fail);
            jout.put("error",e);


        } catch (ParseException e) {
            e.printStackTrace();
        }
        response.getOutputStream().println(String.valueOf(jout));

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
            jout.put(status,fail);
            jout.put("error",e);


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



            if (pathInfo == null || pathInfo.equals("/")) {
                out = "Welcome Home";

            } else if (pathInfo.equals("/user")) {
                if (payload.get("action").equals("deleteuser")) {

                    String username=String.valueOf(payload.get("username"));
                    selectuser.setString(1,username);
                    ResultSet rs=selectuser.executeQuery();
                    Boolean userexist=false;
                    while(rs.next()){
                        userexist=true;
                        break;
                    }
                    if(userexist) {
                        deleteuser.setString(1, username);
                        deleteuser.executeUpdate();
                        jout.put(status,succcess);
                    }
                    else {
                        jout.put(status,fail);
                        jout.put("reason","No such a username");
                    }
                }

            }
            else if (pathInfo.equals("/contact")) {
                if (payload.get("action").equals("deletecontact")) {
                    String username = String.valueOf(payload.get("username"));
                    String contactname = String.valueOf(payload.get("contactname"));
                    JSONObject contact=null;
                    if (!username.equals(contactname)){
                        selectuser.setString(1,username);
                        ResultSet rs=selectuser.executeQuery();
                        Boolean userexist = false;
                        while (rs.next()) {
                            contact = (JSONObject) new JSONParser().parse((String) rs.getObject(3));
                            userexist = true;
                            break;
                        }
                        if (!userexist) {
                            jout.put(status, fail);
                            jout.put("reason", "No such a username exist");
                        } else {
                            selectuser.setString(1, contactname);
                            rs = selectuser.executeQuery();
                            userexist = false;
                            while (rs.next()) {
                                userexist = true;
                                break;
                            }
                            if (!userexist) {
                                jout.put(status, fail);
                                jout.put("reason", "No such a contactname exist");
                            } else{
                                JSONArray arr = (JSONArray) contact.get("contact");
                                if (arr.contains(contactname)) {
                                    arr.remove(arr.indexOf(contactname));
                                    contact.put("contact",arr);
                                    addcontact.setObject(1,contact.toString());
                                    addcontact.setString(2,username);
                                    addcontact.executeUpdate();
                                    jout.put(status, succcess);
                                } else {
                                    jout.put(status, fail);
                                    jout.put("reason", "No such a contactname exist in your contacts");
                                }
                            }

                        }
                    }
                    else{
                        jout.put(status,fail);
                        jout.put("reason","user and contact usernames are same");
                    }

                }

            }




        } catch (SQLException e) {
            e.printStackTrace();
            jout.put(status,fail);
            jout.put("error",e);


        } catch (ParseException e) {
            e.printStackTrace();
        }
        response.getOutputStream().println(String.valueOf(jout));

    }
}
