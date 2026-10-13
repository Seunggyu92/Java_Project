package com.volunteerintegration.common;

import java.util.List;

/** 목록 화면 한 페이지 결과와 페이지 번호 계산. page는 1부터 시작한다. */
public class PageResult<T> {

    private static final int PAGE_BLOCK = 10;

    private final List<T> content;
    private final int page;
    private final int size;
    private final long total;

    public PageResult(List<T> content, int page, int size, long total) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return (int) Math.max(1, (total + size - 1) / size);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public boolean isHasPrev() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < getTotalPages();
    }

    /** 페이지 번호 묶음의 시작 (1~10, 11~20 ...). */
    public int getStartPage() {
        return ((page - 1) / PAGE_BLOCK) * PAGE_BLOCK + 1;
    }

    public int getEndPage() {
        return Math.min(getStartPage() + PAGE_BLOCK - 1, getTotalPages());
    }

    /** SQL OFFSET 값. */
    public static int offset(int page, int size) {
        return (Math.max(page, 1) - 1) * size;
    }
}
