; What Arch Anchor reads of a PHP file (#85), as a tree-sitter query over the grammar of
; tree-sitter-php. TreeSitterPhpSourceReader runs it and gives each capture its meaning;
; the capture names below are the ones it looks for.
;
; The query is checked against the grammar when it is loaded: a node or field that the
; grammar does not have fails there, with its position in this file, instead of silently
; matching nothing.

; ---------------------------------------------------------------------------------------
; Namespaces. `namespace App\Models;` has no body and holds what follows it;
; `namespace App\Models { ... }` holds its body; `namespace { ... }` is the global one.

(namespace_definition
  name: (namespace_name)? @namespace.name
  body: (compound_statement)? @namespace.body) @namespace

; ---------------------------------------------------------------------------------------
; Imports of classes. `use function` and `use const` carry a `type` field, on the
; statement when a group follows and on the clause otherwise, so both must be without one.
; The anchor (.) takes the first name of a clause: the second one, if any, is its alias.

; use A\B;   use A\B as C;   use A\B, C\D;
(namespace_use_declaration
  !type
  (namespace_use_clause
    !type
    . [(name) (qualified_name)] @import.name
    alias: (name)? @import.alias))

; use A\{B, C\D as E};
(namespace_use_declaration
  !type
  (namespace_name) @import.prefix
  body: (namespace_use_group
    (namespace_use_clause
      !type
      . [(name) (qualified_name) (namespace_name)] @import.name
      alias: (name)? @import.alias)))

; ---------------------------------------------------------------------------------------
; What declares a class. An anonymous class (`new class { ... }`) is not here: what it
; names belongs to the declaration it is written in.

(class_declaration name: (name) @declaration.name) @declaration
(interface_declaration name: (name) @declaration.name) @declaration
(trait_declaration name: (name) @declaration.name) @declaration
(enum_declaration name: (name) @declaration.name) @declaration

; ---------------------------------------------------------------------------------------
; Where PHP expects a class. A name is `Order`, a qualified name `Models\Order` or
; `\App\Models\Order`, a relative name `namespace\Order`. Nothing else is captured: a
; variable (`new $class`), an expression (`new (static::class)`), and `self`, `static`
; and `parent` in the places where the grammar gives them a node of their own.

; A type: of a property, a parameter, a return, a constant or a catch, alone or inside a
; nullable, union, intersection or DNF type. Scalar types are primitive_type, not this.
(named_type [(name) (qualified_name) (relative_name)] @reference)

; class A extends B   interface A extends B, C   new class extends B
(base_clause [(name) (qualified_name) (relative_name)] @reference)

; class A implements B, C   enum A implements B
(class_interface_clause [(name) (qualified_name) (relative_name)] @reference)

; use SomeTrait, Other;   Only the direct children: `foo as bar` inside the braces names
; methods, and `A::foo insteadof B` is read as a class constant access, below.
(use_declaration [(name) (qualified_name) (relative_name)] @reference)

; #[Attribute]   #[Attribute(arguments)]
(attribute . [(name) (qualified_name) (relative_name)] @reference)

; new Order(...)
(object_creation_expression . [(name) (qualified_name) (relative_name)] @reference)

; Order::find(...)   Order::find(...) as a first-class callable
(scoped_call_expression scope: [(name) (qualified_name) (relative_name)] @reference)

; Order::$count
(scoped_property_access_expression scope: [(name) (qualified_name) (relative_name)] @reference)

; Order::STATUS   Order::class   Status::Open
(class_constant_access_expression . [(name) (qualified_name) (relative_name)] @reference)

; $x instanceof Order
(binary_expression
  operator: "instanceof"
  right: [(name) (qualified_name) (relative_name)] @reference)
