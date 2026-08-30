# Java Tutorial | Tutorial 2 Java Basics

Welcome to the second ThunderChickens Java Tutorial.
This tutorial will answer these questions:

1. What are data types?
2. How do I create variables?
3. What are classes?
4. What are objects?
5. What are loops?
6. What are constants?
7. What is Camel Case?

There's a lot to do, so let's get started!

## What are data types?

Java is special in that you have to declare variable types before the variable name. If you're familiar with JavaScript, you can just do "var num = 0" or "var welcome = "hello". With Java, it's a little more complicated. In Java, you have to say what your variable is. Is it a number, string, double, or some other type? Some common data types we use are:

**double** - A type of number capaple of storing decimal numbers with a high degree of precision
**boolean** - True or False
**string** - Like a sentence or word

There are many more than just these data types, but a few that we use the most often. You can code them like this:

```Java
double numberOfNuggets = 10.51;
boolean matchesNumberOnBox = False;
string messageToEmployee = "Get it together";
```

## How do I create variables?

Ah, sweet old Java. It's never simple... Anyway, there are a few "parameters" you can declare before you create your variable.

**public** - Allows you to access the variable from any file (Learn more about this below)
**private** - A variable contained in one file alone
**static** - So that you don't need to instantiate the class to use the variable
**final** - Once the variable is declared, it cannot be changed

## What are classes?

Imagine your room. Now, take everything out of your room so that it is empty. This is what a class is. In a class, you can create functions, variables, loops, really anything. You can have mutliple classes working all together to do multiple different things. But how do we do that? You can code classes like this:

```Java
public class Robot {
 public double numberOfNuggets = 10.51;
 private boolean matchesNumberOnBox = False; // Notice this is private
 public string messageToEmployee = "Get it together";
}
```
 
## What are objects?

An object is an instance of a class. You can't just magically use everything in a class in other classes. You must first create an object! Once you create the object, anything in the class is now accessible through that object. You can code them like this:

```java
Robot robotObject = new Robot();
robotObject.numberOfNuggets; // 10.51
robotObject.matchesNumberOnBox; // Error: Does not exist
robotObject.messageToEmployee; // "Get it together"
```

## What are loops?

The easiest way to understand what a loop is - do something over and over again until someone says to stop. That is what a loop is! It is an iteration of some code over and over again until it is stopped. The most common loop we use is the "for loop". Basically, some code is executed repeatedly a specific number of times until a certain condition is met. Another common loop is the "while loop". To sum this one up, it runs while "x" = "y"; Until "x" does not equal "y", this loop will run. Examples of both loops are below:

```Java
// i++ means increase i by 1 each time.
for (int i = 0; i <= 10; i++) {
 System.out.println(i);
}

// The code below results in an infinite loop.
while (true == true) {

}
```

## What are constants?

Constants are variables that remain- well... constant! We typically put all of these variables in a different file to keep the code nice and squeaky clean. These variables never change!

## What is Camel Case?

Camel Case is a naming convention in which multiple words are joined together without spaces, and each word's first letter is capitalized except for the first word. Adam is a stickler for this! You can learn more about Camel Case here. Here is an example of a Camel Case variable name:

```Java
int camelCaseName = 1;
string anotherCamelCaseName = "Each word after the first is capitalized!";
```

And there ya' go! You are all set to start learning about Java practices! Proceed to the next tutorial to learn about Java practices.

<br>
Made with ♡ by Jacob