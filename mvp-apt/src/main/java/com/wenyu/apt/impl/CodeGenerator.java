package com.wenyu.apt.impl;

import com.wenyu.apt.annotations.MvpPresenter;
import com.wenyu.apt.element.Clazz;
import com.wenyu.apt.element.ModelElement;
import com.wenyu.apt.element.PresenterElement;
import com.wenyu.apt.element.ViewElement;
import com.wenyu.apt.utils.ClazzMapUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;


/**
 * Created by chan on 16/10/15.
 * jiacheng.li@shanbay.com
 */
public class CodeGenerator {
	/**
	 * 用于写java文件
	 */
	private Filer mFiler;
	/**
	 * logger
	 */
	private Messager mMessager;

	/**
	 * APT生成代码所在的包名
	 */
	private String mPackage;

	private PresenterElement mPresenterElement;
	private ViewElement mViewElement;
	private ModelElement mModelElement;

	public CodeGenerator(Filer filer, Messager messager) {
		mFiler = filer;
		mMessager = messager;
	}

	public void generate(Set<? extends Element> presenterSet, Set<? extends Element> modelSet, Set<? extends Element> viewSet) throws IOException {
		parsePresenter(presenterSet);
		parseModel(modelSet);
		parseView(viewSet);
		generate();
	}

	private void parsePresenter(Set<? extends Element> presenterSet) {
		Iterator<? extends Element> iterator = presenterSet.iterator();
		VariableElement variableElement = (VariableElement) iterator.next();

		for (AnnotationMirror annotationMirror : variableElement.getAnnotationMirrors()) {
			if (MvpPresenter.class.getCanonicalName().equals(annotationMirror.getAnnotationType().toString())) {
				Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
				TypeMirror mirror = variableElement.asType();
				mPresenterElement = new PresenterElement(variableElement.getSimpleName().toString(), variableElement.getEnclosingElement().toString(), mirror.toString());
				for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {

					String key = entry.getKey().getSimpleName().toString();
					if ("module".equals(key)) {
						mPresenterElement.module = new Clazz(entry.getValue().toString());
					} else if ("component".equals(key)) {
						mPresenterElement.component = new Clazz(entry.getValue().toString());
					} else if ("dependency".equals(key)) {
						mPresenterElement.dependency = new Clazz(entry.getValue().toString());
					}
				}
			}
		}

		Element element = variableElement.getEnclosingElement();
		while (element != null && element.getEnclosingElement() != null) {
			mPackage = element.toString();
			element = element.getEnclosingElement();
		}

		mPackage = mPackage.substring(0, mPackage.lastIndexOf("."));
	}

	private void parseModel(Set<? extends Element> modelSet) {
		Iterator<? extends Element> iterator = modelSet.iterator();
		if (!iterator.hasNext()) {
			return;
		}

		VariableElement variableElement = (VariableElement) iterator.next();
		TypeMirror mirror = variableElement.asType();
		mModelElement = new ModelElement(variableElement.getSimpleName().toString(), variableElement.getEnclosingElement().toString(), mirror.toString());
	}

	private void parseView(Set<? extends Element> viewSet) {
		Iterator<? extends Element> iterator = viewSet.iterator();
		VariableElement variableElement = (VariableElement) iterator.next();
		TypeMirror mirror = variableElement.asType();
		mViewElement = new ViewElement(variableElement.getSimpleName().toString(), variableElement.getEnclosingElement().toString(), mirror.toString());
	}

	public void generate() throws IOException {
		//指定java文件写入的位置
		String clazzName = ClazzMapUtils.getClazzName(mPresenterElement.enclosingClazzName);
		JavaFileObject javaFileObject = mFiler.createSourceFile(mPackage + "." + clazzName);

		//开始写文件
		Writer writer = javaFileObject.openWriter();
		writer.write(generateSourceCode(mPackage, clazzName));
		writer.flush();
		writer.close();
	}


	private String generateSourceCode(String packageName, String clazzName) {

		//包
		StringBuilder stringBuilder = new StringBuilder("package ");
		stringBuilder.append(packageName);
		stringBuilder.append(";\n");


		stringBuilder.append("public class ");
		stringBuilder.append(clazzName);
		stringBuilder.append("{");

		stringBuilder.append("public static void inject(");
		stringBuilder.append(mPresenterElement.enclosingClazzName);
		stringBuilder.append(" o");
		stringBuilder.append(",");
		if (mPresenterElement.dependency != null) {
			stringBuilder.append(mPresenterElement.dependency.getCanonicalName());
			stringBuilder.append(" o1");
		} else {
			stringBuilder.append("Object o1");
		}
		stringBuilder.append("){");

		String daggerComponentName = mPresenterElement.component.getPackage() + ".Dagger" + mPresenterElement.component.getSimpleName();
		String builderName = daggerComponentName + ".Builder";

		stringBuilder.append(builderName);
		stringBuilder.append(" builder = ");
		stringBuilder.append(daggerComponentName);
		stringBuilder.append(".builder();\n");


		stringBuilder.append(mPresenterElement.component.getCanonicalName());
		stringBuilder.append(" component = builder.");
		stringBuilder.append(simpleName2FunctionName(mPresenterElement.module.getSimpleName()));
		stringBuilder.append("(new ");
		stringBuilder.append(mPresenterElement.module.getCanonicalName());
		stringBuilder.append("(o))");
		if (mPresenterElement.dependency != null) {
			mMessager.printMessage(Diagnostic.Kind.NOTE, "dependency not null");
			stringBuilder.append(".");
			stringBuilder.append(simpleName2FunctionName(mPresenterElement.dependency.getSimpleName()));
			stringBuilder.append("(o1)");
		}
		stringBuilder.append(".build();");

		stringBuilder.append("component.inject(o);");
		stringBuilder.append("o.");
		stringBuilder.append(mPresenterElement.fieldName);
		stringBuilder.append("= new ");
		stringBuilder.append(mPresenterElement.type);

		stringBuilder.append("(o.");
		stringBuilder.append(mViewElement.fieldName);
		stringBuilder.append(",");
		if (mModelElement == null) {
			stringBuilder.append("null");
		} else {
			stringBuilder.append("o.");
			stringBuilder.append(mModelElement.fieldName);
		}
		stringBuilder.append(");");

		//结尾
		stringBuilder.append("}}");
		return stringBuilder.toString();
	}

	public static String simpleName2FunctionName(String simpleName) {

		if (simpleName == null || simpleName.isEmpty()) {
			return simpleName;
		}

		if (simpleName.length() == 1) {
			return String.valueOf(Character.toLowerCase(simpleName.charAt(0)));
		}

		return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
	}
}
