package net.zorgblub.typhonkai.epub;

import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;

import net.nightwhistler.htmlspanner.FontResolver;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.SpanStack;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import net.zorgblub.typhonkai.Configuration;
import net.zorgblub.typhonkai.Typhon;
import net.zorgblub.typhonkai.epub.furigana.FuriganaSpan;

import org.htmlcleaner.BaseToken;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by Benjamin on 5/7/2016.
 */
public class TyphonHtmlSpanner extends HtmlSpanner implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    Configuration config;

    private boolean skipFurigana = false;

    private boolean addParenthesis = false;

    private int furiganaColor = -1;


    public TyphonHtmlSpanner() {
        super();
        init();
    }

    public TyphonHtmlSpanner(HtmlCleaner cleaner, FontResolver fontResolver) {
        super(cleaner, fontResolver);
        init();
    }

    private void init() {
        Typhon.getComponent().inject(this);
        initFurigana(config.isFuriganaEnabled());
        registerHandler("rp", new DeleteNodeHandler());
        registerHandler("rt", new DeleteNodeHandler());
        registerHandler("rtc", new DeleteNodeHandler());

        config.registerOnSharedPreferenceChangeListener(this);
    }

    public void initFurigana(boolean enabled) {
        if (enabled) {
            registerHandler("ruby", new RubyNodeHandler());
        } else {
            unregisterHandler("ruby");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(Configuration.KEY_FURIGANA_ENABLED)){
            initFurigana(config.isFuriganaEnabled());
        }
    }

    @Override
    public boolean isStripExtraWhiteSpace() {
        return super.isStripExtraWhiteSpace();
    }


    private static class DeleteNodeHandler extends TagNodeHandler {
        @Override
        public boolean rendersContent() {
            return true;
        }

        @Override
        public void handleTagNode(TagNode node, SpannableStringBuilder builder, int start, int end, SpanStack spanStack) {
            // Delete
        }
    }

    private static class RubyNodeHandler extends TagNodeHandler {
        private void parseRBRubyNode(TagNode node, SpannableStringBuilder builder, int start, int end, SpanStack spanStack) {
            TagNode[] kanjiNodes = node.getElementsByName("rb", true);
            TagNode[] furiganaNodes = node.getElementsByName("rt", true);
            for (int i = 0; i < kanjiNodes.length && i < furiganaNodes.length; i++) {
                String kanji = kanjiNodes[i].getText().toString();
                String furigana = furiganaNodes[i].getText().toString();
                // Trim and replace if needed to avoid spaces ruining our dictionary lookups
                if (kanji.trim().length() < kanji.length()) {
                    builder.replace(start, start + kanji.length(), kanji.trim());
                    end -= kanji.length();
                    kanji = kanji.trim();
                }
                spanStack.pushSpan(new FuriganaSpan(kanji, furigana), start, start += kanji.length());
            }
        }

        private void parseRubyNode(TagNode node, SpannableStringBuilder builder, int start, int end, SpanStack spanStack) {
            List<? extends BaseToken> children = node.getAllChildren();
            String kanji = "";
            for (BaseToken child : children) {
                if (child instanceof ContentNode) {
                    ContentNode contentNode = (ContentNode) child;
                    kanji = contentNode.getContent();
                } else if (child instanceof TagNode) {
                    TagNode tagNode = (TagNode) child;
                    if (tagNode.getName().equals("rt")) {
                        String furigana = tagNode.getText().toString();
                        // Trim and replace if needed to avoid spaces ruining our dictionary lookups
                        if (kanji.trim().length() < kanji.length()) {
                            builder.replace(start, start + kanji.length(), kanji.trim());
                            end -= kanji.length();
                            kanji = kanji.trim();
                        }
                        spanStack.pushSpan(new FuriganaSpan(kanji, furigana), start, start += kanji.length());
                    }
                }
            }
        }

        @Override
        public void handleTagNode(TagNode node, SpannableStringBuilder builder, int start,
                                  int end, SpanStack spanStack) {
            // Both rbc and rb are deprecated according to mdn, however, we support them anyways.
            // Note that rbc is only somewhat supported as rtc is not handled.
            if (node.getElementsByName("rb", true).length > 0) {
                parseRBRubyNode(node, builder, start, end, spanStack);
            } else {
                parseRubyNode(node, builder, start, end, spanStack);
            }
        }
    }
}
