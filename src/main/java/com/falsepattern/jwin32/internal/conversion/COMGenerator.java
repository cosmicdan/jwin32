/*
 * Copyright (c) 2021 FalsePattern
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.falsepattern.jwin32.internal.conversion;

import com.falsepattern.jwin32.internal.conversion.common.*;

import java.lang.reflect.Method;
import java.util.Arrays;

public class COMGenerator {

    private static final CParameter MEMORY_ADDRESS_PARAM = new CParameter(CType.MEMORY_ADDRESS, "address" +
                                                                                                "");


    private static final CField SCOPE = new CField();
    private static final CField OBJ = new CField();
    private static final CField VTBL = new CField();
    static {
        SCOPE.accessSpecifier.fin = OBJ.accessSpecifier.fin = VTBL.accessSpecifier.fin = true;
        OBJ.accessSpecifier.vis = AccessSpecifier.Visibility.PUBLIC;
        SCOPE.type = CType.MEMORY_SESSION;
        SCOPE.name = "scope";
        SCOPE.initializer.append("ResourceScope.newImplicitScope()");

        OBJ.type = CType.MEMORY_ADDRESS;
        OBJ.name = "obj";

        VTBL.type = CType.MEMORY_SEGMENT;
        VTBL.name = "vtbl";
    }

    public static CClass generateCOM(String pkg, Class<?> baseClass, Class<?> vtbl) {
        var com = new CClass();
        com.name = baseClass.getSimpleName() + "_J";
        com.pkg = pkg;
        com.accessSpecifier.vis = AccessSpecifier.Visibility.PUBLIC;
        com.importImplicitly(new CType(baseClass));
        com.importImplicitly(new CType(vtbl));
        com.importImplicitly(CType.WIN32);
        com.addField(SCOPE);
        com.addField(OBJ);
        com.addField(VTBL);
        var ifMethods = getInterfaceMethods(vtbl);
        com.addConstructor(getConstructor(baseClass, vtbl, ifMethods));
        if (!com.name.equals("ID3DInclude_J"))
            com.addMethod(getREFIIDMethod(baseClass));
        for (Method method : ifMethods) {
            var wrapper = toWrapper(method);
            if (wrapper == null) {
                System.err.println("Failed to generate wrapper method for " + baseClass.getSimpleName() + "." + method.getName() + "!");
                continue;
            }
            com.addField(toField(method));
            com.addMethod(wrapper);
        }
        return com;
    }

    private static CMethod getREFIIDMethod(Class<?> baseClass) {
        var method = new CMethod();
        method.accessSpecifier.vis = AccessSpecifier.Visibility.PUBLIC;
        method.accessSpecifier.stat = true;
        method.returnType = CType.MEMORY_SEGMENT;
        method.name = "REFIID";
        method.code.append("return ").append(CType.WIN32.simpleName()).append(".");
        if (baseClass.getSimpleName().equals("XMLDOMDocumentEvents")) {
            method.code.append("D");
        }
        method.code.append("IID_").append(baseClass.getSimpleName()).append("$SEGMENT();");
        return method;
    }

    private static CField toField(Method method) {
        var field = new CField();
        field.accessSpecifier.fin = true;
        field.type = new CType(method.getReturnType());
        field.name = method.getName();
        return field;
    }

    private static CConstructor getConstructor(Class<?> baseClass, Class<?> vtbl, Method[] ifMethods) {
        var vtblName = vtbl.getSimpleName();
        var constructor = new CConstructor();
        constructor.accessSpecifier.vis = AccessSpecifier.Visibility.PUBLIC;
        constructor.paramList.add(MEMORY_ADDRESS_PARAM);
        constructor.code
                .append("var segment = ").append(baseClass.getSimpleName()).append(".ofAddress(address, scope);")
                .append("this.obj = address;\n"
                + "vtbl = ").append(vtblName).append(".ofAddress(").append(baseClass.getSimpleName()).append(".lpVtbl$get(segment), scope);\n");
        for (var method: ifMethods) {
            constructor.code.append(method.getName()).append(" = ").append(vtblName).append(".").append(method.getName()).append("(vtbl);\n");
        }
        return constructor;
    }

    private static Method[] getInterfaceMethods(Class<?> vtbl) {
        var enclosedClasses = Arrays.asList(vtbl.getClasses());
        return Arrays.stream(vtbl.getMethods())
                .filter((method) -> enclosedClasses.stream().anyMatch((clazz) -> clazz.getSimpleName().equals(method.getName()) && clazz.equals(method.getReturnType())))
                .toArray(Method[]::new);
    }

    private static CMethod toWrapper(Method getter) {
        var methodInterface = getter.getReturnType();
        var optMethod = Arrays.stream(methodInterface.getMethods()).filter((method -> method.getName().equals("apply"))).findFirst();
        if (optMethod.isEmpty()) return null;
        var interfaceMethod = optMethod.get();
        var wrapper = new CMethod();
        wrapper.accessSpecifier.vis = AccessSpecifier.Visibility.PUBLIC;
        wrapper.name = methodInterface.getSimpleName();
        wrapper.returnType = new CType(interfaceMethod.getReturnType());
        var callParamList = new CParamList();
        callParamList.add(new CParameter(CType.MEMORY_ADDRESS, "obj"));
        var params = Arrays.asList(interfaceMethod.getParameters());
        params = params.subList(1, params.size());
        params.forEach((param) -> {
            var cParam = new CParameter(new CType(param.getType()), param.getName());
            wrapper.paramList.add(cParam);
            callParamList.add(cParam);
        });
        if (!interfaceMethod.getReturnType().equals(void.class)) {
            wrapper.code.append("return ");
        }
        wrapper.code.append(wrapper.name).append(".apply(").append(callParamList.asFunctionCallParams()).append(");");
        return wrapper;
    }
}
