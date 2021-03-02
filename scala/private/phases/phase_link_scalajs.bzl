#
# PHASE: link scalajs jars
#
# 
#
def _colon_paths(data):
    return ":".join([
        f.path
        for f in sorted(data)
    ])

def phase_link_scalajs(ctx, p):
    """
    Links scala.js IR into vanilla javascript.
    """

    output_js = ctx.outputs.js
    runtime_jars = sorted(p.collect_jars.transitive_runtime_jars.to_list())

    worker_content = "{output}\n{main_class}\n{method}\n{include_jars}".format(
        output = output_js.path,
        main_class = getattr(ctx.attr, "main_class", ""),
        method = getattr(ctx.attr, "method", ""),
        include_jars = _colon_paths(runtime_jars),
    )
    argfile = ctx.actions.declare_file(
        "%s_worker_input" % ctx.label.name,
        sibling = output_js,
    )
    ctx.actions.write(output = argfile, content = worker_content)
    ctx.actions.run(
        inputs = runtime_jars + [argfile],
        outputs = [output_js],
        executable = ctx.attr._scalajs_linker.files_to_run,
        mnemonic = "ScalaJSLink",
        progress_message = "Linking scala.js file: %s" % ctx.label,
        execution_requirements = {"supports-workers": "1"},
        arguments = ["@" + argfile.path],
    )
