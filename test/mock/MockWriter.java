package mock;

import java.io.IOException;
import java.io.Writer;

public class MockWriter extends Writer {
    private String content;

    public MockWriter() {
        content = "";
    }

    public void write(String s) {
        content += s;
    }

    public String getContent() {
        return content;
    }

    public void newLine() {}

    @Override
    public void write(char[] chars, int i, int i1) throws IOException {

    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
