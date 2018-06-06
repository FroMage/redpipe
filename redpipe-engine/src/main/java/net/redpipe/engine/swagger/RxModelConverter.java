package net.redpipe.engine.swagger;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RxModelConverter implements ModelConverter {

    private static final List<String> skippableClasses = new ArrayList<>();
    static {
        skippableClasses.add(Single.class.getName());
        skippableClasses.add(Observable.class.getName());
        skippableClasses.add(rx.Single.class.getName());
        skippableClasses.add(rx.Observable.class.getName());

    }


    @Override
    public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> chain) {
        if (type instanceof ParameterizedType) {
            ParameterizedType param = (ParameterizedType) type;
            if (skippableClasses.contains(param.getRawType().getTypeName())) {
                return chain.next().resolveProperty(param.getActualTypeArguments()[0], context, annotations, chain);
            }
        }
        return chain.next().resolveProperty(type, context, annotations, chain);
    }

    @Override
    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (type instanceof ParameterizedType) {
            ParameterizedType param = (ParameterizedType) type;
            if (skippableClasses.contains(param.getRawType().getTypeName())) {
                return chain.next().resolve(param.getActualTypeArguments()[0], context, chain);
            }
        }
        return chain.next().resolve(type, context, chain);
    }

}
