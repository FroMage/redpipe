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

    private static final List<String> delegateToFirstTypeArg = new ArrayList<>();
    static {
        delegateToFirstTypeArg.add(Single.class.getName());
        delegateToFirstTypeArg.add(Observable.class.getName());
        delegateToFirstTypeArg.add(rx.Single.class.getName());
        delegateToFirstTypeArg.add(rx.Observable.class.getName());
    }


    @Override
    public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> chain) {
        Type delegateType = type;
        if (type instanceof ParameterizedType) {
            ParameterizedType param = (ParameterizedType) type;
            if (delegateToFirstTypeArg.contains(param.getRawType().getTypeName())) {
                delegateType = param.getActualTypeArguments()[0];
            }
        }
        return chain.next().resolveProperty(delegateType, context, annotations, chain);
    }

    @Override
    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Type delegateType = type;
        if (type instanceof ParameterizedType) {
            ParameterizedType param = (ParameterizedType) type;
            if (delegateToFirstTypeArg.contains(param.getRawType().getTypeName())) {
                delegateType = param.getActualTypeArguments()[0];
            }
        }
        return chain.next().resolve(delegateType, context, chain);
    }

}
