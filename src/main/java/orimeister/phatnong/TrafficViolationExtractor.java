package orimeister.phatnong;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TrafficViolationExtractor {
    public List<TrafficViolation> extractTrafficViolations(String html) {
        List<TrafficViolation> violations = new ArrayList<>();

        Document doc = Jsoup.parse(html);
        Element container = doc.getElementById("bodyPrint123");

        if (container == null) {
            System.out.println("No violations container found");
            return violations;
        }

        TrafficViolation currentViolation = new TrafficViolation();
        Elements formGroups = container.select(".form-group");

        for (Element group : formGroups) {
            Element label = group.selectFirst("label span");
            Element valueDiv = group.selectFirst(".col-md-9");

            if (label == null || valueDiv == null) {
                continue;
            }

            String labelText = label.text().trim();
            String value = valueDiv.text().trim();


            if (labelText.contains("Thời gian vi phạm")) {
                if (currentViolation.getDateTime() != null) {
                    violations.add(currentViolation);
                    currentViolation = new TrafficViolation();
                }
                currentViolation.setDateTime(value);
            } else if (labelText.contains("Địa điểm vi phạm")) {
                currentViolation.setLocation(value);
            } else if (labelText.contains("Hành vi vi phạm")) {
                currentViolation.setViolationType(value);
            } else if (labelText.contains("Trạng thái")) {
                currentViolation.setStatus(value);
            }
        }

        if (currentViolation.getDateTime() != null) {
            violations.add(currentViolation);
        }

        System.out.println("Extracted " + violations.size() + " violation(s)");
        return violations;
    }
}
