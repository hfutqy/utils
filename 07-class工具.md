```
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;

import com.lz.lsf.util.StringUtil;

public class ClassUtil {

    static Set<Class> privateTypes = new HashSet<Class>();
    static {
        privateTypes.add(int.class);
        privateTypes.add(double.class);
        privateTypes.add(long.class);
        privateTypes.add(float.class);
        privateTypes.add(boolean.class);
        privateTypes.add(Integer.class);
        privateTypes.add(Double.class);
        privateTypes.add(Long.class);
        privateTypes.add(Float.class);
        privateTypes.add(String.class);
        privateTypes.add(Date.class);
        privateTypes.add(Boolean.class);
    }

 
    public static Class<?> getFieldGenricType(Field field, int index) {
        String signature = field.toGenericString();
        return getGenericeType(signature, index);
    }

    private static Class<?> getGenericeType(String signature, int index) {
        String genericStr = signature.substring(signature.indexOf("<") + 1, signature.indexOf(">"));
        String[] types = genericStr.split(",");
        if (types.length > 0 && types.length > index) {
            try {
                return Class.forName(types[index]);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            return null;
        }
    }

    /**
     * 通过反射, 获得定义Class时声明的父类的泛型参数的类型. 如无法找到, 返回Object.class.
     * 
     * @param clazz
     *            clazz The class to introspect
     * @param index
     *            the Index of the generic ddeclaration,start from 0.
     * @return the index generic declaration, or Object.class if cannot be determined
     */
    @SuppressWarnings("unchecked")
    public static Class<?> getSuperClassGenricType(final Class clazz, final int index) {
        Class<?> ret = null;
        // 返回表示此 Class 所表示的实体（类、接口、基本类型或 void）的直接超类的 Type。
        Type genType = clazz.getGenericSuperclass();
        ret = getGenericType(genType, index);

        if (ret == null) {
            for (Type t : clazz.getGenericInterfaces()) {
                ret = getGenericType(t, index);
                if (ret != null) {
                    break;
                }
            }
        }

        return ret;
    }

    private static Class<?> getGenericType(Type type, int index) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        // 返回表示此类型实际类型参数的 Type 对象的数组。
        Type[] params = ((ParameterizedType) type).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            return null;
        }
        if (!(params[index] instanceof Class)) {
            return null;
        }

        return (Class) params[index];
    }

    public static String getSetterMethod(String attrName) {
        String fst = attrName.substring(0, 1).toUpperCase();
        attrName = fst + attrName.substring(1);
        return "set" + attrName;
    }

    public static void setObjectValue(Object obj, String fieldName, Object value) {
        try {
            String methodName = getSetterMethod(fieldName);
            Method method = findMethod(obj.getClass(), methodName, String.class);
            if (method != null) {
                method.invoke(obj, value);
            }
            else {
                Field field = obj.getClass().getDeclaredField(fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    field.set(obj, value);
                }
                else {
                    throw new RuntimeException("no field or set method found for field:" + fieldName);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPriovateType(Class type) {
        return privateTypes.contains(type);
    }

    public static Object getPrivateTypeValue(Class type, String value) {
        if (String.class == type) {
            return value;
        }
        else if (int.class == type) {
            return StringUtil.isEmpty(value) ? 0 : Integer.parseInt(value);
        }
        else if (double.class == type) {
            return StringUtil.isEmpty(value) ? 0 : Double.parseDouble(value);
        }
        else if (long.class == type) {
            return StringUtil.isEmpty(value) ? 0 : Long.parseLong(value);
        }
        else if (float.class == type) {
            return StringUtil.isEmpty(value) ? 0 : Float.parseFloat(value);
        }
        else if (Integer.class == type) {
            return StringUtil.isEmpty(value) ? 0 : Integer.valueOf(value);
        }
        else if (Double.class == type) {
            return StringUtil.isEmpty(value) ? 0 : Double.valueOf(value);
        }
        else if (Long.class == type) {
            return StringUtil.isEmpty(value) ? 0 : Long.valueOf(value);
        }
        else if (Float.class == type) {
            return StringUtil.isEmpty(value) ? 0 : Float.valueOf(value);
        }
        else if (BigDecimal.class == type) {
            return StringUtil.isEmpty(value) ? BigDecimal.ZERO :  BigDecimal.valueOf(Double.valueOf(value));
        }
        else if (boolean.class == type || Boolean.class == type) {
            return StringUtil.isEmpty(value) ? false : Boolean.valueOf(value);
        }
        else {
            return null;
        }
    }
    
    public static void main(String[] args) {
		System.out.println( Boolean.valueOf("true"));
		System.out.println( Boolean.valueOf("false"));
		String[] sp = "|1|2|||| ".split("\\|");
		System.out.println(sp);
		System.out.println("|1|2||||".endsWith("|"));
	}

    public static Method findMethod(Class<?> clazz, String methodName, Class<?> paramType) {
        Method ret = null;
        try {
            ret = clazz.getMethod(methodName, paramType);
        }
        catch (Exception e) {
            if (paramType.getSuperclass() != null) {
                ret = findMethod(clazz, methodName, paramType.getSuperclass());
            }
            if (ret == null) {
                for (Class _clazz : paramType.getInterfaces()) {
                    ret = findMethod(clazz, methodName, _clazz);
                    if (ret != null) {
                        break;
                    }
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cloneInstance(T obj) {
        T ret;
        try {
            ret = (T) BeanUtils.cloneBean(obj);
        }
        catch (Exception e) {
            throw new RuntimeException("clone instance failed!", e);
        }

        return ret;
    }
}
```
