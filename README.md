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
		imageGetter = new ImageGetter(YourActivity.this, true, false, false, 1, 1,
				RequestCodes.REQUEST_CODE_SELECT_PICTURE.value, RequestCodes.REQUEST_CODE_CROP.value, getImageGetterCallback());
    
    		yourView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//Optional, or here
				imageGetter = new ImageGetter(YourActivity.this, true, false, false, 1, 1,
					RequestCodes.REQUEST_CODE_SELECT_PICTURE.value, RequestCodes.REQUEST_CODE_CROP.value, getImageGetterCallback());
				imageGetter.start("");
			}
		});
  }
  
  private ImageGetter.Callbacks getImageGetterCallback() {
		return new ImageGetter.Callbacks(){
			@Override
			public Activity getActivity() {
				return YourActivity.this;
			}

			@Override
			public void setImage(final String filename, String tag) {
				//Get image from path and do whatever you want with it.
				//For example, load it into an imageView
				ImageUtils.setImageFromMemory(YourActivity.this, filename, imageView);
			}
		};
	}
  
	@Override
	public void onBackPressed() {
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
