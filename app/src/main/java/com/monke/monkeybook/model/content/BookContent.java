package com.monke.monkeybook.model.content;

import com.hwangjr.rxbus.RxBus;
import com.monke.monkeybook.bean.BookContentBean;
import com.monke.monkeybook.bean.BookSourceBean;
import com.monke.monkeybook.bean.ChapterListBean;
import com.monke.monkeybook.dao.ChapterListBeanDao;
import com.monke.monkeybook.dao.DbHelper;
import com.monke.monkeybook.help.RxBusTag;
import com.monke.monkeybook.model.AnalyzeRule.AnalyzeElement;
import com.monke.monkeybook.model.ErrorAnalyContentManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import io.reactivex.Observable;

public class BookContent {
    private String tag;
    private BookSourceBean bookSourceBean;

    BookContent(String tag, BookSourceBean bookSourceBean) {
        this.tag = tag;
        this.bookSourceBean = bookSourceBean;
    }

    public Observable<BookContentBean> analyzeBookContent(final String s, final String durChapterUrl, final int durChapterIndex) {
        return Observable.create(e -> {
            BookContentBean bookContentBean = new BookContentBean();
            bookContentBean.setDurChapterIndex(durChapterIndex);
            bookContentBean.setDurChapterUrl(durChapterUrl);
            bookContentBean.setTag(tag);
            String ruleBookContent = bookSourceBean.getRuleBookContent();
            if (ruleBookContent.startsWith("$")) {
                ruleBookContent = ruleBookContent.substring(1);
            }
            try {
                Document doc = Jsoup.parse(s);
                AnalyzeElement analyzeElement = new AnalyzeElement(doc, durChapterUrl);
                bookContentBean.setDurChapterContent(analyzeElement.getResult(ruleBookContent));
                bookContentBean.setRight(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                ErrorAnalyContentManager.getInstance().writeNewErrorUrl(durChapterUrl);
                bookContentBean.setDurChapterContent(durChapterUrl.substring(0, durChapterUrl.indexOf('/', 8)) + ex.getMessage());
                bookContentBean.setRight(false);
            }
            e.onNext(bookContentBean);
            e.onComplete();
        });
    }

    public Observable<BookContentBean> upChapterList(BookContentBean bookContentBean) {
        return Observable.create(e -> {
            if (bookContentBean.getRight()) {
                ChapterListBean chapterListBean = DbHelper.getInstance().getmDaoSession().getChapterListBeanDao().queryBuilder()
                        .where(ChapterListBeanDao.Properties.DurChapterUrl.eq(bookContentBean.getDurChapterUrl())).unique();
                if (chapterListBean != null) {
                    bookContentBean.setNoteUrl(chapterListBean.getNoteUrl());
                    chapterListBean.setHasCache(true);
                    DbHelper.getInstance().getmDaoSession().getChapterListBeanDao().update(chapterListBean);
                    RxBus.get().post(RxBusTag.CHAPTER_CHANGE, chapterListBean);
                }
            }
            e.onNext(bookContentBean);
            e.onComplete();
        });
    }
}
