package tech.picktime.ageCompute;

import java.util.List;

/**
 * Created by jsb-hdp-0 on 2017/9/21.
 * 用于解析 face++ 的返回json
 */

public class JsonParse {
    private String image_id;
    private String request_id;
    private int time_used;
    private List<Face> faces;

    public Face getFace(int index){
        return faces.get(index);
    }
}

class Face{
    private Attributes attributes;
    private Face_rectangle face_rectangle;
    private String face_token;

    public Attributes getAttributes(){
        return attributes;
    }

    public Face_rectangle getFace_rectangle(){
        return face_rectangle;
    }
}

class Attributes{
    private Gender gender;
    private Age age;
    private Beauty beauty;

    public Gender getGender(){
        return gender;
    }

    public Age getAge(){
        return age;
    }
}

class Face_rectangle{
    private int width;
    private int height;
    private int top;
    private int left;

    public int getWidth(){
        return width;
    }

    public int getHeight(){
        return height;
    }

    public int getTop(){
        return top;
    }

    public int getLeft(){
        return left;
    }
}

class Gender{
    private String value;

    public String getValue(){
        return value;
    }
}

class Age{
    private int value;

    public int getValue(){
        return value;
    }
}

class Beauty{
    private float female_score;
    private float male_score;
}