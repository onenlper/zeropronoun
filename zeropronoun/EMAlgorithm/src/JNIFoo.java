public class JNIFoo {

	// gcc -shared -fpic -o libfoo.so -I /usr/local/jdk1.6.0_21/include/linux/
	// -I /usr/local/jdk1.6.0_21/include/ foo.c
	public native String nativeFoo();

	static {
		System.loadLibrary("foo");
	}

	public void print() {
		String str = nativeFoo();
		System.out.println(str);
	}

	public static void main(String[] args) {
		(new JNIFoo()).print();
		return;
	}
}
