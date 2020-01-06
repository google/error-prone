package com.google.errorprone.descriptionlistener;

import com.google.errorprone.DescriptionListener;

public interface CustomDescriptionListenerFactory {
  DescriptionListener createFactory(DescriptionListenerResources resources);
}
