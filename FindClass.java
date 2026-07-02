import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
public class FindClass {
    public static void main(String[] args) throws Exception {
        String cp = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("cp.txt")));
        for(String path : cp.split(";")) {
            if(path.contains("spring-ai-ollama")) {
                JarFile jar = new JarFile(path);
                Enumeration<JarEntry> e = jar.entries();
                while(e.hasMoreElements()) {
                    String name = e.nextElement().getName();
                    if(name.contains("OllamaOptions")) System.out.println(name);
                }
            }
        }
    }
}
