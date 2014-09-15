package com.ft.methodearticletransformer.methode;

import static com.ft.methodearticletransformer.methode.EomFileType.EOMCompoundStory;

public class SupportedTypeResolver {
	
	private final String fileType;

	public SupportedTypeResolver(String fileType) {
		this.fileType = fileType;
	}

	
	public boolean isASupportedType(){
		return isAStory();
	}
	
	private boolean isAStory(){
        return EOMCompoundStory.getTypeName().equals(fileType);
	}

}
