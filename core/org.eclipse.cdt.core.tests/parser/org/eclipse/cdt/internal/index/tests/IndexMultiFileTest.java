/*******************************************************************************
 * Copyright (c) 2013, 2015 Google, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *	   Sergey Prigogin (Google) - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.index.tests;

import junit.framework.TestSuite;

/**
 * Index tests involving multiple header and source files.
 *
 * The first line of each comment section preceding a test contains the name of the file
 * to put the contents of the section to. To request the AST of a file, put an asterisk after
 * the file name.
 */
public class IndexMultiFileTest extends IndexBindingResolutionTestBase {

	public IndexMultiFileTest() {
		setStrategy(new SinglePDOMTestNamedFilesStrategy(true));
	}

	public static TestSuite suite() {
		return suite(IndexMultiFileTest.class);
	}

	// A.h
	//	template <typename T>
	//	struct A {
	//	  void m(T p);
	//	};

	// B.h
	//	struct B {};

	// confuser.cpp
	//	#include "A.h"
	//
	//	namespace {
	//	struct B {};
	//	}
	//	A<B*> z;

	// test.cpp *
	//	#include "A.h"
	//	#include "B.h"
	//
	//	void test(A<B*> a, B* b) {
	//	  a.m(b);
	//	}
	public void testAnonymousNamespace_416278() throws Exception {
		checkBindings();
	}

	// a.h
	//	namespace ns1 {
	//	}
	//
	//	namespace ns2 {
	//	namespace ns3 {
	//	namespace waldo = ns1;
	//	}
	//	}

	// a.cpp
	//	#include "a.h"

	// b.h
	//	namespace ns1 {
	//	}
	//
	//	namespace ns2 {
	//	namespace waldo = ns1;
	//	}

	// b.cpp
	//	#include "b.h"
	//
	//	namespace ns2 {
	//	namespace waldo = ::ns1;
	//	}

	// c.cpp
	//	namespace ns1 {
	//	class A {};
	//	}
	//
	//	namespace waldo = ns1;
	//
	//	namespace ns2 {
	//	namespace ns3 {
	//	waldo::A a;
	//	}
	//	}
	public void testNamespaceAlias_442117() throws Exception {
		checkBindings();
	}

	// confuser.cpp
	//	namespace ns1 {
	//	namespace waldo {}
	//	namespace ns2 {
	//	namespace waldo {}
	//	}
	//	}

	// test.cpp *
	//	namespace waldo {
	//	class A {};
	//	}
	//
	//	namespace ns1 {
	//	namespace ns2 {
	//
	//	waldo::A* x;
	//
	//	}
	//	}
	public void testNamespace_481161() throws Exception {
		checkBindings();
	}

	// A.h
	//	#include "B.h"
	//
	//	template <typename T, typename U>
	//	struct A {
	//	  A(D<U> p);
	//	};
	//
	//	template <>
	//	struct A<B, C> {
	//	  A(D<C> p);
	//	};

	// B.h
	//	struct B {};
	//
	//	struct C {};
	//
	//	template <typename T>
	//	struct D {};

	// C.h
	//	class B;

	// confuser.cpp
	//	#include "B.h"

	// test.cpp *
	//	#include "A.h"
	//	#include "C.h"
	//
	//	void test() {
	//	  D<C> x;
	//	  new A<B, C>(x);
	//	}
	public void testExplicitSpecialization_494359() throws Exception {
		checkBindings();
	}

	// test1.h
	//	namespace ns {
	//
	//	struct C {
	//	  friend class B;
	//	};
	//
	//	}

	// test2.h
	//	class B {};
	//
	//	namespace ns {
	//
	//	struct A {
	//	  operator B();
	//	};
	//
	//	}
	//
	//	void waldo(B);

	// confuser.cpp
	//	#include "test1.h"

	// test.cpp *
	//	#include "test1.h"
	//	#include "test2.h"
	//
	//	void test(ns::A a) {
	//	  waldo(a);
	//	}
	public void testFriendClassDeclaration_508338() throws Exception {
		checkBindings();
	}

	// test.h
	//	friend int operator*(double, C) { return 0; }
	
	// test.cpp *
	//	namespace N {
	//
	//	    struct unrelated {};
	//
	//	    struct B {
	//	        friend int operator*(unrelated, unrelated) { return 0; }
	//	    };
	//	}
	//	    
	//	template <typename = int>
	//	struct C : public N::B {
	//	    #include "test.h"
	//	};
	//	template <typename> struct Waldo;
	//	Waldo<decltype(0.5 * C<>{})> w;
	public void testFriendFunctionInHeaderIncludedAtClassScope_509662() throws Exception {
		checkBindings();
	}

	// test.h
	//	template <typename T>
	//	struct atomic;
	//
	//	template <typename T>
	//	struct atomic<T*>;
	
	// test1.cpp
	//	#include "test.h"
	
	// test2.cpp *
	//	#include "test.h"
	//
	//	template <typename T>
	//	struct atomic {};
	//
	//	template <typename T>
	//	struct atomic<T*> {
	//		void fetch_sub();
	//	};
	public void testClassTemplatePartialSpecialization_470726() throws Exception {
		checkBindings();
	}
	
	// test.h
	//	template <bool = false>
	//	struct base {};
	//
	//	template <bool B = false>
	//	struct derived : private base<B> {
	//	    constexpr derived() : base<B>() {}
	//	};
	
	// test1.cpp
	//	#include "test.h"
	
	// test2.cpp *
	//	template <typename = void>
	//	struct base {};
	//
	//	static derived<> waldo;
	public void testProblemBindingInMemInitList_508254() throws Exception {
		// This code is invalid, so we don't checkBindings().
		// If the test gets this far (doesn't throw in setup() during indexing), it passes.
	}
}
