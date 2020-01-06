package com.google.errorprone.descriptionlistener;

import com.google.auto.service.AutoService;
import com.google.errorprone.DescriptionListener;

@AutoService(CustomDescriptionListenerFactory.class)
public class JavacErrorCustomDescriptionListenerFactory implements CustomDescriptionListenerFactory {
  @Override
  public DescriptionListener createFactory(DescriptionListenerResources resources) {
    return new JavacErrorDescriptionListener(resources);
  }
}
