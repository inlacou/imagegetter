# imagegetter
[![](https://jitpack.io/v/inlacou/imagegetter.svg)](https://jitpack.io/#inlacou/imagegetter)

In your module:app build.gradle:
```groovy
dependencies {
    ...
    compile 'com.github.inlacou:imagegetter:RC3'
}
```

In your project build.gradle:
```groovy
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

In your activity:
```java
  protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Optional, here:
		imageGetter = new ImageGetter(NewDebateActivity.this, true, false, false, 1, 1,
				RequestCodes.REQUEST_CODE_SELECT_PICTURE.value, RequestCodes.REQUEST_CODE_CROP.value, getImageGetterCallback());
    
    yourView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//Optional, or here
				imageGetter.start("");
			}
		});
  }
  
  private ImageGetter.Callbacks getImageGetterCallback() {
		return new ImageGetter.Callbacks(){
			@Override
			public Activity getActivity() {
				return NewDebateActivity.this;
			}

			@Override
			public void setImage(final String filename, String tag) {
				ViewImagePreview imagePreview = new ViewImagePreview(NewDebateActivity.this, new ViewImagePreview.Callbacks() {
					@Override
					public boolean onSurfaceClick(Object item) {
						UrlActivity.navigate(NewDebateActivity.this, filename, "TODO");
						return true;
					}

					@Override
					public void onDelete(Object item) {
						deleteImageFromS3(mediaKey);
						NewDebateActivity.this.filename = null;
						urlPreviewContainer.removeAllViews();
					}

					@Override
					public boolean isDeleteable() {
						return true;
					}

					@Override
					public String getData() {
						return filename;
					}

					@Override
					public ViewImagePreview.Source getSource() {
						return ViewImagePreview.Source.device;
					}
				});
				urlPreviewContainer.addView(imagePreview);
				Log.d(DEBUG_TAG+".AMAZON.start", "mediaKey: " + mediaKey);
				if(NewDebateActivity.this.filename!=null && !NewDebateActivity.this.filename.equalsIgnoreCase(filename)){
					deleteImageFromS3(mediaKey);
				}
				NewDebateActivity.this.filename = filename;
				imagePreview.populate();

				uploadToS3(filename);
			}
		};
	}
  
	@Override
	public void onBackPressed() {
		deleteImageFromS3(mediaKey);
		//If you want ImageGetter to delete the image from external memory (recommended)
		if(imageGetter!=null) imageGetter.destroy();
	}
  
  @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		...
		imageGetter.onActivityResult(requestCode, resultCode, data);
	}
  
  @Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		...
		imageGetter.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		...
		if(imageGetter!=null)
			imageGetter.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		...
		imageGetter = ImageGetter.onRestoreInstanceState(savedInstanceState, getImageGetterCallback());
	}
```
