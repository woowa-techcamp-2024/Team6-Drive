package com.woowacamp.storage.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.woowacamp.storage.global.aop.type.FieldType;

/**
 * 권한을 확인해야 하는 File, Folder에 해당 어노테이션을 명시하면, 그 값을 바탕으로 권한을 검증한다.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckField {
	FieldType value();
}
