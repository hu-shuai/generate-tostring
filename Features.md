# Introduction #
The generate toString() plugin has the following features:

# Features #
  * quick generation of toString() methods
  * 6 out-of-the-box templates to choose for code layout in the generate toString() methods
  * generated code can be customized using powerful Velocity macro language
  * default templates supports different code styles:
    * string concat (+)
    * StringBuilder (JDK 1.5)
    * org.apache.commons.lang.toStringBuilder()
    * with/without @Override annotations
    * with/without super.toString()
  * field chooser dialogs (just like generate getter/setter)
  * confirm options can be default set so you wont be bothered by questions "toString() method already exists"
  * getter methods can also be threated as fields and thus be obligated for output in the generated code
  * exclusion of certain fields can be set (eg. loggers, constant fields). Can be set using regular expression to give you the full power
  * quick selection list of templates to choose when the action is invoked, so you at runtime can choose what template/code style
  * option to automatic sort elements in the generated code
  * inspections to inspect for:
    * classes without toString() methods
    * classes that has out of sync existing toString() methods
  * on-the-fly inspections to let you know if you the fields you class have defined is out of sync with your toString() method
  * a lot of class introspected context to give you full power to customize the code generation in Velocity ($class.hasSuper(), etc.)
  * full plugin documentation included
  * The plugin has its own settings (ctrl + alt + s)
  * And many other minor "hidden" details added from community feedback.