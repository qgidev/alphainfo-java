import io.alphainfo.AlphaInfoClient;
import io.alphainfo.Models.AnalyzeRequest;
import io.alphainfo.Models.AnalysisResult;
import java.util.ArrayList;
import java.util.List;

/**
 * alphainfo — Hello World (Java).
 *
 *   ALPHAINFO_API_KEY=ai_... ./gradlew runQuickstart
 *
 * Get a free key at https://alphainfo.io/register.
 */
public class Quickstart {
    public static void main(String[] args) {
        String apiKey = System.getenv("ALPHAINFO_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Set ALPHAINFO_API_KEY first: https://alphainfo.io/register");
            System.exit(1);
        }

        // Toy signal: sine that abruptly changes amplitude at the midpoint.
        List<Double> signal = new ArrayList<>(400);
        for (int i = 0; i < 200; i++) signal.add(Math.sin(i / 10.0));
        for (int i = 0; i < 200; i++) signal.add(Math.sin(i / 10.0) * 3);

        try (AlphaInfoClient client = new AlphaInfoClient(apiKey)) {
            AnalysisResult r = client.analyze(
                    new AnalyzeRequest().signal(signal).samplingRate(100));
            System.out.printf("structural_score: %.3f%n", r.structuralScore);
            System.out.println("confidence_band:  " + r.confidenceBand);
            System.out.println("change_detected:  " + r.changeDetected);
            System.out.println("analysis_id:      " + r.analysisId);
        }
    }
}
