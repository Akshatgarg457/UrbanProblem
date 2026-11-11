package org.springframework.data.domain;
import java.util.*; import java.util.function.Function;
public interface Page<T> extends Iterable<T> { <U> Page<U> map(Function<? super T, ? extends U> f); }
