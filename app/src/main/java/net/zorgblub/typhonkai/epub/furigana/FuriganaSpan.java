/*

 */

package net.zorgblub.typhonkai.epub.furigana;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.style.LineHeightSpan;
import android.text.style.ReplacementSpan;

import net.zorgblub.typhonkai.Configuration;
import net.zorgblub.typhonkai.Typhon;

import javax.inject.Inject;


public class FuriganaSpan extends ReplacementSpan implements LineHeightSpan.WithDensity {

    @Inject
    Configuration config;

    @Inject
    Context context;

    private float kanjiTextSize;
    private float furiganaTextSize;
    private final String kanji;
    private final String furigana;

    public FuriganaSpan(String kanji, String furigana) {
        Typhon.getComponent().inject(this);
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        kanjiTextSize = config.getTextSize() * scaledDensity;
        furiganaTextSize = config.getTextSize() * config.getFuriganaSizeRatio() * scaledDensity;
        this.kanji = kanji;
        this.furigana = furigana;
    }

    @Override
    public void chooseHeight(CharSequence charSequence, int start, int end, int spanstartv,
                             int lineHeight, Paint.FontMetricsInt fm, TextPaint paint) {
        TextPaint furiganaPaint = new TextPaint(paint);
        furiganaPaint.setTextSize(furiganaTextSize);

        Paint.FontMetricsInt kanjiFM = paint.getFontMetricsInt();
        Paint.FontMetricsInt furiganaFM = furiganaPaint.getFontMetricsInt();

        // Furigana's descent will be lined up with kanji's ascent
        int neededAscent = kanjiFM.ascent + (furiganaFM.ascent - fm.descent);
        int neededTop = kanjiFM.ascent + (furiganaFM.top - fm.descent);

        // If we already have enough space, don't bother creating more.
        fm.ascent = Math.min(fm.ascent, neededAscent);
        fm.top = Math.min(fm.top, neededTop);
    }

    @Override
    public void chooseHeight(CharSequence charSequence, int start, int end, int spanstartv,
                             int lineHeight, Paint.FontMetricsInt fm) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(kanjiTextSize);
        chooseHeight(charSequence, start, end, spanstartv, lineHeight, fm, paint);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        TextPaint furiganaPaint = new TextPaint(paint);
        furiganaPaint.setTextSize(furiganaTextSize);

        int kanjiSize = (int) paint.measureText(kanji);
        int furiganaSize = (int) (furiganaPaint.measureText(furigana) + 0.5f);
        return Math.max(kanjiSize, furiganaSize);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int kanjiBaseline, int bottom, Paint paint) {
        TextPaint furiganaPaint = new TextPaint(paint);
        furiganaPaint.setTextSize(furiganaTextSize);

        Paint.FontMetrics kanjiFM = paint.getFontMetrics();
        Paint.FontMetrics furiganaFM = furiganaPaint.getFontMetrics();

        // Furigana's descent should line up with kanji's ascent
        float kanjiAscent = kanjiBaseline + kanjiFM.ascent;
        float furiganaBaseline = kanjiAscent - furiganaFM.descent;

        float kanjiSize = paint.measureText(kanji);
        float furiganaSize = furiganaPaint.measureText(furigana);

        float furiganaXOffset = (kanjiSize - furiganaSize) / 2;

        canvas.drawText(kanji, x, kanjiBaseline, paint);
        canvas.drawText(furigana, x + furiganaXOffset, furiganaBaseline, furiganaPaint);
    }
}
