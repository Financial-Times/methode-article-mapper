package com.ft.methodetransformer.methode;

import static com.ft.methodetransformer.methode.EomFileType.EOMCompoundStory;

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
