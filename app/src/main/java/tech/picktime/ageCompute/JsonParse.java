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
}

 class Face{
    private Attributes attributes;
    private Face_rectangle face_rectangle;
    private String face_token;
}

class Attributes{
    private Gender gender;
    private Age age;
    private Beauty beauty;
}

class Face_rectangle{
    private int width;
    private int height;
    private int top;
    private int left;
}

class Gender{
    private String value;
}

class Age{
    private String value;
}

class Beauty{
    private float female_score;
    private float male_score;
}