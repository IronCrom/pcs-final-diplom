import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class BooleanSearchEngine implements SearchEngine {

    private final Map<String, List<PageEntry>> index;

    public BooleanSearchEngine(File pdfsDir) throws IOException {
        this.index = new HashMap<>();
        for (File file : Objects.requireNonNull(pdfsDir.listFiles())) {
            PdfDocument doc = new PdfDocument(new PdfReader(file));
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                String text = PdfTextExtractor.getTextFromPage(doc.getPage(i));
                String[] words = text.split("\\P{IsAlphabetic}+");
                Map<String, Integer> wordsCount = new HashMap<>();
                for (String word : words) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    word = word.toLowerCase();
                    wordsCount.put(word, wordsCount.getOrDefault(word, 0) + 1);
                }
                int finalI = i;
                wordsCount.forEach((word, count) -> {
                    PageEntry pageEntry = new PageEntry(file.getName(), finalI, count);
                    List<PageEntry> pageEntryList = new ArrayList<>();
                    if (index.containsKey(word)) {
                        pageEntryList = index.get(word);
                        pageEntryList.add(pageEntry);
                    } else {
                        pageEntryList.add(pageEntry);
                    }
                    index.put(word, pageEntryList);
                });
            }
        }
    }

    @Override
    public List<PageEntry> search(String word) {
        word = word.toLowerCase();
        if (index.get(word) == null) {
            return Collections.emptyList();
        } else {
            List<PageEntry> sortedIndex = index.get(word);
            Collections.sort(sortedIndex);
            return sortedIndex;
        }
    }

    public List<PageEntry> searchRequest(String request, String stopListFileName) throws IOException {
        Set<String> words = new HashSet<>(List.of(request.split("\\P{IsAlphabetic}+")));
        List<String> stopWords = Files.readAllLines(Paths.get(stopListFileName));
        words.removeIf(word -> stopWords.contains(word.toLowerCase()));
        List<PageEntry> sortedIndex = new ArrayList<>();
        words.forEach(word -> {
                    if (sortedIndex.isEmpty()) {
                        sortedIndex.addAll(search(word));
                    } else {
                        List<PageEntry> searchIndex = search(word);
                        searchIndex.forEach(indexEntry -> {
                                    if (sortedIndex.contains(indexEntry)) {
                                        PageEntry pageEntry = sortedIndex.get(sortedIndex.indexOf(indexEntry));
                                        sortedIndex.set(sortedIndex.indexOf(indexEntry), pageEntry.countSum(indexEntry));
                                    } else {
                                        sortedIndex.add(indexEntry);
                                    }
                                }
                        );
                    }
                }
        );
        Collections.sort(sortedIndex);
        return sortedIndex;
    }
}
