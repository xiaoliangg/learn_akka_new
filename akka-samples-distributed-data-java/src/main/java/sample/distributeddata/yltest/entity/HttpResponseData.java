package sample.distributeddata.yltest.entity;

/**
 * @description: TODO
 * @author: xiaoliang
 * @date: 2022/8/12 11:39
 **/
public class HttpResponseData {
    public String eventTime;
    public String result;

    public HttpResponseData(String eventTime, String result) {
        this.eventTime = eventTime;
        this.result = result;
    }

    @Override
    public String toString() {
        return "HttpResponseData{" +
                "eventTime='" + eventTime + '\'' +
                ", result='" + result + '\'' +
                '}';
    }
}
