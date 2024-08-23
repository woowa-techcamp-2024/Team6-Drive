package com.woowacamp.storage.global.aop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.woowacamp.storage.global.annotation.CheckDto;
import com.woowacamp.storage.global.annotation.CheckField;
import com.woowacamp.storage.global.annotation.RequestType;
import com.woowacamp.storage.global.aop.type.FieldType;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionCheckAspect {
	private final PermissionHandler permissionHandler;

	/**
	 * 해당 파일에 대한 권한이 있는지 확인하는 메소드
	 * 권한이 있는지 확인 후(hasPermission 호출 후) 요청한 사용자의 정보를 파일이나 폴더의 ownerId로 변경해준다.
	 */
	@Around("@annotation(requestType)")
	public Object checkPermission(ProceedingJoinPoint joinPoint, RequestType requestType) throws Throwable {

		PermissionFieldsDto permissionFieldsDto = new PermissionFieldsDto();

		MethodSignature signature = (MethodSignature)joinPoint.getSignature();
		Parameter[] parameters = signature.getMethod().getParameters();
		Object[] args = joinPoint.getArgs();

		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			Object arg = args[i];

			CheckField checkField = parameter.getAnnotation(CheckField.class);
			if (checkField != null) {
				setField(permissionFieldsDto, checkField.value().getValue(), arg);
			}

			CheckDto checkDto = parameter.getAnnotation(CheckDto.class);
			if (checkDto != null) {
				processClassFields(arg, permissionFieldsDto);
			}

		}

		// 권한 인증 진행
		permissionHandler.hasPermission(requestType.permission(), requestType.fileType(), permissionFieldsDto);

		updateMethodParameters(parameters, args, permissionFieldsDto);

		return joinPoint.proceed(args);
	}

	private void updateMethodParameters(Parameter[] parameters, Object[] args,
		PermissionFieldsDto permissionFieldsDto) {
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			CheckField checkField = parameter.getAnnotation(CheckField.class);
			if (checkField != null) {
				FieldType fieldType = checkField.value();
				Object newValue = null;
				switch (fieldType) {
					case USER_ID:
						newValue = permissionFieldsDto.getOwnerId();
						break;
					case FILE_ID:
						newValue = permissionFieldsDto.getFileId();
						break;
					case FOLDER_ID:
						newValue = permissionFieldsDto.getFolderId();
						break;
					case MOVE_FOLDER_ID:
						newValue = permissionFieldsDto.getMoveFolderId();
						break;
					case CREATOR_ID:
						newValue = permissionFieldsDto.getUserId();
						break;
				}
				if (newValue != null) {
					// updateParameterValue(args, parameter, newValue);
					args[i] = convertValueIfNeeded(newValue, parameter.getType());
				}
			}

			CheckDto checkDto = parameter.getAnnotation(CheckDto.class);
			if (checkDto != null) {
				Object requestDto = args[i];
				Class<?> clazz = requestDto.getClass();
				if (clazz.isRecord()) {
					RecordComponent[] components = clazz.getRecordComponents();
					Object[] currentValues = new Object[components.length];
					try {
						for (int j = 0; j < components.length; j++) {
							RecordComponent component = components[j];
							Method accessor = component.getAccessor();
							currentValues[j] = accessor.invoke(requestDto);
						}

						boolean valueChanged = false;
						for (int j = 0; j < components.length; j++) {
							Field field = clazz.getDeclaredField(components[j].getName());
							CheckField dtoCheckField = field.getAnnotation(CheckField.class);
							if (dtoCheckField != null) {
								Object newValue = null;
								FieldType fieldType = dtoCheckField.value();
								switch (fieldType) {
									case USER_ID:
										newValue = permissionFieldsDto.getOwnerId();
										break;
									case FILE_ID:
										newValue = permissionFieldsDto.getFileId();
										break;
									case FOLDER_ID:
										newValue = permissionFieldsDto.getFolderId();
									case MOVE_FOLDER_ID:
										newValue = permissionFieldsDto.getMoveFolderId();
										break;
									case CREATOR_ID:
										newValue = permissionFieldsDto.getUserId();
										break;
								}
								if (newValue != null && !newValue.equals(currentValues[j])) {
									currentValues[j] = convertValueIfNeeded(newValue, components[j].getType());
									valueChanged = true;
								}
							}
						}

						if (valueChanged) {
							Constructor<?> constructor = clazz.getDeclaredConstructor(
								Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new)
							);
							args[i] = constructor.newInstance(currentValues);
						}
					} catch (Exception e) {
						log.error("[Reflection Error] 요청 DTO record 클래스 생성 중 예외 발생", e);
						throw ErrorCode.PERMISSION_CHECK_FAILED.baseException();
					}

				}
			}
		}
	}

	/**
	 * 데이터를 클래스 필드의 타입에 맞춰 반환하는 메소드
	 */
	private Object convertValueIfNeeded(Object value, Class<?> targetType) {
		// 문자열 변환
		if (targetType == String.class) {
			return value.toString();
		}

		// 기본 타입 및 래퍼 클래스 변환
		if (targetType == Boolean.class || targetType == boolean.class) {
			return Boolean.valueOf(value.toString());
		} else if (targetType == Byte.class || targetType == byte.class) {
			return Byte.valueOf(value.toString());
		} else if (targetType == Short.class || targetType == short.class) {
			return Short.valueOf(value.toString());
		} else if (targetType == Integer.class || targetType == int.class) {
			return Integer.valueOf(value.toString());
		} else if (targetType == Long.class || targetType == long.class) {
			return Long.valueOf(value.toString());
		} else if (targetType == Float.class || targetType == float.class) {
			return Float.valueOf(value.toString());
		} else if (targetType == Double.class || targetType == double.class) {
			return Double.valueOf(value.toString());
		} else if (targetType == Character.class || targetType == char.class) {
			String s = value.toString();
			if (s.length() != 1) {
				throw new IllegalArgumentException("Cannot convert to char: " + s);
			}
			return s.charAt(0);
		}

		// Enum 변환
		if (targetType.isEnum()) {
			return Enum.valueOf((Class<Enum>)targetType, value.toString());
		}
		// 필요에 따라 다른 타입 변환 로직 추가
		log.error("[Reflection Error] 요청 데이터 타입 변환 중 예외 발생, value = {}, targetClassType = {}", value, targetType);
		throw ErrorCode.PERMISSION_CHECK_FAILED.baseException();
	}

	/**
	 * 권한 인증용 클래스에 필요한 데이터를 추가하는 메소드
	 *
	 * @param permissionFieldsDto 권한 인증에 사용할 클래스
	 * @param fieldName 요청으로 들어온 필드의 이름
	 * @param value 요청으로 들어온 필드의 값
	 */
	private void setField(PermissionFieldsDto permissionFieldsDto, String fieldName, Object value) {
		switch (fieldName) {
			case "userId":
				permissionFieldsDto.setUserId((Long)value);
				break;
			case "folderId":
				permissionFieldsDto.setFolderId((Long)value);
				break;
			case "fileId":
				permissionFieldsDto.setFileId((Long)value);
				break;
			case "moveFolderId":
				permissionFieldsDto.setMoveFolderId((Long)value);
				break;
			case "creatorId":
				permissionFieldsDto.setCreatorId((Long)value);
				break;
			default:
				throw ErrorCode.PERMISSION_CHECK_FAILED.baseException("요청 파라미터 AOP 처리 시 예외 발생, 필드 이름 = {}", fieldName);
		}
	}

	/**
	 * 파라미터 타입이 별도의 클래스인 경우 필요한 값을 추출하는 메소드
	 * @param requestDto 컨트롤러의 파라미터가 별도의 DTO인 객체
	 * @param permissionFieldsDto 권한 인증에 사용할 클래스
	 */
	private void processClassFields(Object requestDto, PermissionFieldsDto permissionFieldsDto) {
		if (requestDto == null)
			return;

		Class<?> clazz = requestDto.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			CheckField checkField = field.getAnnotation(CheckField.class);
			if (checkField != null) {
				field.setAccessible(true);
				try {
					Object value = field.get(requestDto);
					setField(permissionFieldsDto, checkField.value().getValue(), value);
				} catch (IllegalAccessException e) {
					log.error("[PermissionCheckAspect] request dto AOP 처리 시 예외 발생, error message = {}", e.getMessage());
					throw ErrorCode.PERMISSION_CHECK_FAILED.baseException();
				}
			}
		}

	}

}
