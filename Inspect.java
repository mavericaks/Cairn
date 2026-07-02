import java.lang.reflect.Method;
public class Inspect {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("org.springframework.ai.ollama.api.OllamaChatOptions$Builder");
        for(Method m : clazz.getMethods()) {
            System.out.println(m.getName());
        }
    }
}
