package pl.tomaszko.s03e04.web;

public class SearchResponse {

    private String output;

    public SearchResponse() {
    }

    public SearchResponse(String output) {
        this.output = output;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
