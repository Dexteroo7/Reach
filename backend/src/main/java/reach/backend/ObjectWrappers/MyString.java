package reach.backend.ObjectWrappers;

/**
 * Created by dexter on 28/12/14.
 */
public class MyString {

    private final String string;

    public MyString(String string) {

        this.string = string;
    }

//    public MyString(Object object) {
//
//        this.string = object.toString();
//    }

    public String getString() {
        return string;
    }
}
