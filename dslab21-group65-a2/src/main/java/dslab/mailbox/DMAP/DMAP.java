package dslab.mailbox.DMAP;

import dslab.Message.Message;
import dslab.mailbox.tcp.dmapThread;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DMAP {

    private static final int LOGGEDOUT = 0;
    private static final int LOGGEDIN = 1;

    private static final int SHOWING = 2;
    private static final int LISTING = 3;

    private int state = LOGGEDOUT;
    private String user;
    private dmapThread thread;

    private HashMap<Integer,Message> list;
    private Message message;

    public DMAP(dmapThread thread){
        this.thread = thread;
    }

    public String processInput(String input){
        String output = "";

        switch(state){
            case LOGGEDOUT:
                if(input == null)
                    output = "ok DMAP";
                else {
                    String newInput = input;
                    String[] splitInput = input.split("\\s");

                    if(splitInput.length > 1)
                        newInput = splitInput[0];
                    switch(newInput.toLowerCase(Locale.ROOT)){
                        case "login":
                            if(splitInput.length <3){
                                output = "error not enough data";
                                break;
                            }
                            String name = splitInput[1];
                            String password = splitInput[2];

                            //check user
                            int ans = thread.checkLogin(name,password);

                            switch(ans){
                                case 0:
                                    output = "error unknown user";
                                    break;
                                case 1:
                                    output = "error wrong password";
                                    break;
                                case 2:
                                    output = "ok";
                                    state = LOGGEDIN;
                                    user = name;
                                    break;
                            }
                            break;

                        case "show":
                        case "delete":
                        case "list":
                        case "logout":
                            output = "error not logged in";
                            break;
                        case "quit":
                            output = "ok bye";
                            break;
                        default:
                            output = "error protocol error";
                            break;
                    }
                }
            break;
            case LOGGEDIN:
                if(input != null){
                    String newInput = input;
                    String[] splitInput = input.split("\\s");

                    if(splitInput.length > 1)
                        newInput = splitInput[0];
                    switch(newInput.toLowerCase(Locale.ROOT)) {
                        case "login":
                            output = "error already logged in";
                            break;
                        case "logout":
                            output = "ok";
                            user = "";
                            state = LOGGEDOUT;
                            break;
                        case "quit":
                            output = "ok bye";
                            break;
                        case "show":
                            if (splitInput.length < 2) {
                                output = "error too little arguments";
                                break;
                            }
                            int id;
                            try {
                                id = Integer.parseInt(splitInput[1]);
                            } catch (NumberFormatException e){
                                output = "error wrong format id";
                                break;
                            }
                            Message message = this.thread.showMessage(id, this.user);
                            if (message == null){
                                output = "error unknown message id";
                                break;
                            }

                            this.message = message;
                            output = "from: " + message.getSender();
                            this.state = SHOWING;
                            break;
                        case "list":
                            this.list = this.thread.getAllMessages(this.user);
                            if(list == null) {
                                output = "no mails available";
                                break;
                            }
                            if(!list.isEmpty()){
                                this.state = LISTING;
                                int curid = list.keySet().iterator().next();
                                Message msg = list.get(curid);
                                output = curid + " " + msg.getSender() + " " + msg.getSubject();
                                list.remove(curid);
                            }
                            break;
                        case "delete":
                            if (splitInput.length < 2) {
                                output = "error too little arguments";
                                break;
                            }
                            try {
                                id = Integer.parseInt(splitInput[1]);
                            } catch (NumberFormatException e){
                                output = "error wrong format id";
                                break;
                            }
                            boolean ans = this.thread.delete(id, user);
                            if(ans)
                                output="ok";
                            else
                                output ="error during deleting";
                            break;
                        default:
                            output = "error protocol error";
                            break;
                    }
                }
                break;
            case SHOWING:
                if(input != null) {
                    switch(input){
                        case "0":
                            output = "to: ";
                            List<String> recips = message.getRecipients();
                            for (String recip: recips) {
                                output += recip + ",";
                            }
                            break;
                        case "1":
                            output = "subject " + message.getSubject();
                            break;
                        case "2":
                            output = "data " + message.getData();
                            this.message = null;
                            this.state = LOGGEDIN;
                            break;
                    }
                }
                break;
            case LISTING: //State, expects null answers from thread to keep listing mails
                if (input == null){
                    if(!list.isEmpty()){
                       int id = list.keySet().iterator().next();
                       Message msg = list.get(id);
                       output = id + " " + msg.getSender() + " " + msg.getSubject();
                       list.remove(id);
                       break;
                    }
                    output = null; //if all mails listed, signal with null.
                    this.list = null;
                    state = LOGGEDIN;
                    break;
                }
                break;
        }

        return output;
    }


}
